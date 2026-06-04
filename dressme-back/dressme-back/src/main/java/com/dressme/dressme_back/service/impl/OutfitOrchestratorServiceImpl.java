package com.dressme.dressme_back.service.impl;

import com.dressme.dressme_back.client.AiOutfitGenerationClient;
import com.dressme.dressme_back.client.DatabaseDressCodeClient;
import com.dressme.dressme_back.client.DatabaseOutfitClient;
import com.dressme.dressme_back.schema.dto.*;
import com.dressme.dressme_back.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutfitOrchestratorServiceImpl implements OutfitOrchestratorService {

    private final WardrobeOrchestratorService  wardrobeService;
    private final AiOutfitGenerationClient     aiClient;
    private final DatabaseOutfitClient         outfitClient;
    private final DatabaseDressCodeClient      dressCodeClient;
    private final ColorScoreService            colorScoreService;
    private final TrendScoreService            trendScoreService;
    private final TasteSimilarityService       tasteSimilarityService;
    private final ScoreEngineService           scoreEngineService;

    @Value("${app.outfit-generation.top-n:5}")
    private int topN;

    // ── Generate ──────────────────────────────────────────────────────────────

    @Override
    public OutfitGenerationResponse generateOutfits(UUID userId, OutfitGenerationRequest request) {
        log.info("Outfit: generateOutfits — usuario={} ocasión={} clima={}", userId, request.occasionId(), request.weatherId());

        // ── Paso 1: cargar guardarropa filtrado con embeddings ────────────────
        List<ClothingEmbeddingInfo> wardrobe = wardrobeService.getWardrobeForOutfit(
                userId, request.occasionId(), request.weatherId());
        log.info("Outfit: {} prendas disponibles tras filtro", wardrobe.size());

        // ── Paso 2: validar slots mínimos ─────────────────────────────────────
        validateSlots(wardrobe);

        // ── Paso 3: resolver embedding del dress code (opcional) ──────────────
        List<Float> dresscodeEmbedding = null;
        if (request.dressCodeId() != null) {
            DressCodeDTO dc = dressCodeClient.getDressCodeById(request.dressCodeId());
            dresscodeEmbedding = floatArrayToList(dc.embeddingVector());
            log.info("Outfit: Dress code {} ({})", dc.name(), request.dressCodeId());
        }

        // ── Paso 4: obtener nombre de ocasión y clima para contexto IA ────────
        CatalogDTO catalog = wardrobeService.getCatalog();
        String occasionName = catalog.occasions().stream()
                .filter(o -> o.id().equals(request.occasionId()))
                .map(CatalogDTO.OccasionEntry::name)
                .findFirst().orElse("Casual");
        String weatherName = catalog.weathers().stream()
                .filter(w -> w.id().equals(request.weatherId()))
                .map(CatalogDTO.WeatherEntry::name)
                .findFirst().orElse("Mild");

        // ── Paso 5: pedir candidatos a IA (con retry 3x backoff) ─────────────
        AiOutfitGenerationRequest aiRequest = new AiOutfitGenerationRequest(
                userId.toString(), wardrobe, occasionName, weatherName, dresscodeEmbedding);
        AiOutfitGenerationResponse aiResponse = callAiWithRetry(aiRequest);

        List<ScoredOutfitCandidate> candidates = aiResponse.candidates();
        log.info("Outfit: {} candidatos recibidos de la IA", candidates.size());

        if (candidates.isEmpty()) {
            return new OutfitGenerationResponse(List.of(), 0);
        }

        // Mapa para lookup rápido de ClothingEmbeddingInfo por clothingId
        Map<UUID, ClothingEmbeddingInfo> wardrobeMap = wardrobe.stream()
                .collect(Collectors.toMap(ClothingEmbeddingInfo::clothingId, c -> c));

        // ── Paso 6: puntuar color y trend en paralelo para todos los candidatos
        List<CompletableFuture<ColorScoreResponse>> colorFutures = candidates.stream()
                .map(c -> CompletableFuture.supplyAsync(
                        () -> colorScoreService.score(buildColorRequest(c, wardrobeMap))))
                .toList();

        List<CompletableFuture<TrendScoreResponse>> trendFutures = candidates.stream()
                .map(c -> CompletableFuture.supplyAsync(
                        () -> trendScoreService.computeTrendScore(getEmbeddings(c, wardrobeMap))))
                .toList();

        // Esperar a que todos los scores paralelos terminen
        CompletableFuture.allOf(
                colorFutures.stream().toArray(CompletableFuture[]::new)).join();
        CompletableFuture.allOf(
                trendFutures.stream().toArray(CompletableFuture[]::new)).join();

        List<ColorScoreResponse> colorScores = colorFutures.stream()
                .map(CompletableFuture::join).toList();
        List<TrendScoreResponse> trendScores = trendFutures.stream()
                .map(CompletableFuture::join).toList();

        // ── Paso 7: persistir todos los candidatos para obtener sus UUIDs ─────
        List<UUID> persistedIds = new ArrayList<>();
        for (int i = 0; i < candidates.size(); i++) {
            ScoredOutfitCandidate c = candidates.get(i);
            List<UUID> clothingIds = c.slots().stream()
                    .map(ScoredOutfitCandidate.CandidateSlot::clothingId).toList();
            OutfitResponse created = outfitClient.createOutfit(new OutfitCreateRequest(
                    userId,
                    clothingIds,
                    request.dressCodeId(),
                    request.occasionId(),
                    request.weatherId(),
                    null,
                    BigDecimal.valueOf(colorScores.get(i).score()).setScale(4, java.math.RoundingMode.HALF_UP),
                    BigDecimal.ZERO, // affinityScore: se actualiza con tasteScore después
                    null,           // totalScore: se actualiza después
                    toFloatArray(c.outfitVector())
            ));
            persistedIds.add(created.id());
        }
        log.info("Outfit: {} candidatos persistidos", persistedIds.size());

        // ── Paso 8: calcular tasteScore en batch (1 sola llamada HTTP) ────────
        List<TasteSimilarityResponse> tasteScores = tasteSimilarityService.computeBatch(
                new TasteSimilarityBatchRequest(userId, persistedIds));

        Map<UUID, Double> tasteByOutfitId = tasteScores.stream()
                .collect(Collectors.toMap(TasteSimilarityResponse::outfitId,
                        TasteSimilarityResponse::similarity));

        // ── Paso 9: totalScore local con ScoreEngine (sin HTTP) ───────────────
        record ScoredEntry(UUID outfitId, double totalScore) {}
        List<ScoredEntry> scored = new ArrayList<>();

        for (int i = 0; i < candidates.size(); i++) {
            UUID outfitId  = persistedIds.get(i);
            double taste   = tasteByOutfitId.getOrDefault(outfitId, 0.0);
            double dresscode = candidates.get(i).dresscodeScore();

            ScoreEngineResponse engineResult = scoreEngineService.scoreFromComponents(
                    colorScores.get(i), dresscode, taste, trendScores.get(i));

            scored.add(new ScoredEntry(outfitId, engineResult.totalScore()));
        }

        // ── Paso 10: ordenar, tomar top-N y eliminar el resto ────────────────
        scored.sort(Comparator.comparingDouble(ScoredEntry::totalScore).reversed());
        Set<UUID> topIds = scored.stream().limit(topN)
                .map(ScoredEntry::outfitId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        scored.stream().skip(topN).forEach(entry -> {
            try {
                outfitClient.deleteOutfit(entry.outfitId(), userId);
                log.debug("Outfit: candidato descartado eliminado — {}", entry.outfitId());
            } catch (Exception e) {
                log.warn("Outfit: no se pudo eliminar candidato {} — {}", entry.outfitId(), e.getMessage());
            }
        });

        // Devolver solo los top-N usando la lista del usuario (una sola llamada HTTP)
        List<OutfitResponse> all = outfitClient.getOutfitsByUser(userId);
        List<OutfitResponse> result = all.stream()
                .filter(o -> topIds.contains(o.id()))
                .sorted(Comparator.comparingInt(o -> new ArrayList<>(topIds).indexOf(o.id())))
                .toList();

        log.info("Outfit: generación completada — {} outfits en top-{}", result.size(), topN);
        return new OutfitGenerationResponse(result, result.size());
    }

    // ── List ──────────────────────────────────────────────────────────────────

    @Override
    public List<OutfitResponse> getOutfits(UUID userId) {
        return outfitClient.getOutfitsByUser(userId);
    }

    // ── Detail ────────────────────────────────────────────────────────────────

    @Override
    public OutfitDetailResponse getOutfitDetail(UUID outfitId) {
        return outfitClient.getOutfitById(outfitId);
    }

    // ── Rate ──────────────────────────────────────────────────────────────────

    @Override
    public OutfitResponse rateOutfit(UUID outfitId, UUID userId, OutfitRatingRequest request) {
        return outfitClient.rateOutfit(outfitId, userId, request);
    }

    // ── Helpers privados ──────────────────────────────────────────────────────

    private void validateSlots(List<ClothingEmbeddingInfo> wardrobe) {
        Set<String> slots = wardrobe.stream()
                .map(ClothingEmbeddingInfo::slot)
                .collect(Collectors.toSet());

        boolean hasSeparates = slots.contains("TOP") && slots.contains("BOTTOM");
        boolean hasOnepiece  = slots.contains("ONEPIECE");

        if (!hasSeparates && !hasOnepiece) {
            List<Map<String, Object>> missing = new ArrayList<>();
            if (!slots.contains("TOP"))    missing.add(Map.of("slot", "TOP",    "needed", 1));
            if (!slots.contains("BOTTOM")) missing.add(Map.of("slot", "BOTTOM", "needed", 1));

            String detail = missing.stream()
                    .map(m -> m.get("needed") + " " + m.get("slot"))
                    .collect(Collectors.joining(", "));

            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "INSUFFICIENT_WARDROBE: necesitas al menos " + detail
                            + " para generar outfits. Sube más prendas.");
        }
    }

    private AiOutfitGenerationResponse callAiWithRetry(AiOutfitGenerationRequest request) {
        long[] delays = {1000L, 2000L, 4000L};
        Exception last = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                return aiClient.generateOutfits(request);
            } catch (Exception e) {
                last = e;
                log.warn("Outfit: intento {}/3 fallido contra dressme-ai: {}", attempt + 1, e.getMessage());
                if (attempt < 2) {
                    try { Thread.sleep(delays[attempt]); }
                    catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                }
            }
        }
        log.error("Outfit: dressme-ai no disponible tras 3 intentos: {}", last != null ? last.getMessage() : "?");
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "AI_UNAVAILABLE: El servicio de IA no está disponible. Intenta más tarde.");
    }

    private ColorScoreRequest buildColorRequest(ScoredOutfitCandidate candidate,
                                                Map<UUID, ClothingEmbeddingInfo> wardrobeMap) {
        List<SlotColorInput> items = candidate.slots().stream()
                .map(slot -> {
                    ClothingEmbeddingInfo info = wardrobeMap.get(slot.clothingId());
                    if (info == null || info.hue() == null) return null;
                    try {
                        Slot slotEnum = Slot.valueOf(slot.slot().toUpperCase());
                        return new SlotColorInput(slotEnum, info.hue(), info.saturation(), info.lightness());
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
        return new ColorScoreRequest(items.isEmpty()
                ? List.of(new SlotColorInput(Slot.TOP, 0, 0, 50)) // fallback neutro
                : items);
    }

    private List<List<Float>> getEmbeddings(ScoredOutfitCandidate candidate,
                                             Map<UUID, ClothingEmbeddingInfo> wardrobeMap) {
        return candidate.slots().stream()
                .map(slot -> wardrobeMap.get(slot.clothingId()))
                .filter(info -> info != null && info.embeddingVector() != null
                        && !info.embeddingVector().isEmpty())
                .map(ClothingEmbeddingInfo::embeddingVector)
                .toList();
    }

    private List<Float> floatArrayToList(float[] arr) {
        if (arr == null) return null;
        List<Float> list = new ArrayList<>(arr.length);
        for (float f : arr) list.add(f);
        return list;
    }

    private float[] toFloatArray(List<Float> list) {
        if (list == null) return new float[0];
        float[] arr = new float[list.size()];
        for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
        return arr;
    }
}
