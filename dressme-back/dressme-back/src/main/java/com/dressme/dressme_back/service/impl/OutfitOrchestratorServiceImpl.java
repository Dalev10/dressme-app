package com.dressme.dressme_back.service.impl;

import com.dressme.dressme_back.client.AiEmbeddingClothingClient;
import com.dressme.dressme_back.client.AiOutfitGenerationClient;
import com.dressme.dressme_back.client.DatabaseDressCodeClient;
import com.dressme.dressme_back.client.DatabaseOutfitClient;
import com.dressme.dressme_back.schema.dto.AiOutfitGenerationRequest;
import com.dressme.dressme_back.schema.dto.AiOutfitGenerationResponse;
import com.dressme.dressme_back.schema.dto.CatalogDTO;
import com.dressme.dressme_back.schema.dto.ClothingDetailResponse;
import com.dressme.dressme_back.schema.dto.ClothingEmbeddingInfo;
import com.dressme.dressme_back.schema.dto.ClothingEmbeddingRequest;
import com.dressme.dressme_back.schema.dto.ClothingEmbeddingResponse;
import com.dressme.dressme_back.schema.dto.ClothingEmbeddingUpdateRequest;
import com.dressme.dressme_back.schema.dto.ColorScoreRequest;
import com.dressme.dressme_back.schema.dto.ColorScoreResponse;
import com.dressme.dressme_back.schema.dto.DressCodeDTO;
import com.dressme.dressme_back.schema.dto.OutfitCreateRequest;
import com.dressme.dressme_back.schema.dto.OutfitDetailResponse;
import com.dressme.dressme_back.schema.dto.OutfitGenerationRequest;
import com.dressme.dressme_back.schema.dto.OutfitGenerationResponse;
import com.dressme.dressme_back.schema.dto.OutfitRatingRequest;
import com.dressme.dressme_back.schema.dto.OutfitResponse;
import com.dressme.dressme_back.schema.dto.OutfitScoreUpdateRequest;
import com.dressme.dressme_back.schema.dto.ScoreEngineResponse;
import com.dressme.dressme_back.schema.dto.Slot;
import com.dressme.dressme_back.schema.dto.SlotColorInput;
import com.dressme.dressme_back.schema.dto.ScoredOutfitCandidate;
import com.dressme.dressme_back.schema.dto.TasteSimilarityBatchRequest;
import com.dressme.dressme_back.schema.dto.TasteSimilarityResponse;
import com.dressme.dressme_back.schema.dto.TrendScoreResponse;
import com.dressme.dressme_back.service.ColorScoreService;
import com.dressme.dressme_back.service.OutfitOrchestratorService;
import com.dressme.dressme_back.service.ScoreEngineService;
import com.dressme.dressme_back.service.TasteSimilarityService;
import com.dressme.dressme_back.service.TrendScoreService;
import com.dressme.dressme_back.service.WardrobeOrchestratorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OutfitOrchestratorServiceImpl implements OutfitOrchestratorService {

        private final WardrobeOrchestratorService wardrobeService;
        private final AiOutfitGenerationClient aiClient;
        private final DatabaseOutfitClient outfitClient;
        private final DatabaseDressCodeClient dressCodeClient;
        private final AiEmbeddingClothingClient embeddingClient;
        private final RestClient databaseClient;
        private final ColorScoreService colorScoreService;
        private final TrendScoreService trendScoreService;
        private final TasteSimilarityService tasteSimilarityService;
        private final ScoreEngineService scoreEngineService;

        @Value("${app.outfit-generation.top-n:5}")
        private int topN;

        public OutfitOrchestratorServiceImpl(
                        WardrobeOrchestratorService wardrobeService,
                        AiOutfitGenerationClient aiClient,
                        DatabaseOutfitClient outfitClient,
                        DatabaseDressCodeClient dressCodeClient,
                        AiEmbeddingClothingClient embeddingClient,
                        RestClient.Builder restClientBuilder,
                        @Value("${app.services.database-url}") String databaseUrl,
                        ColorScoreService colorScoreService,
                        TrendScoreService trendScoreService,
                        TasteSimilarityService tasteSimilarityService,
                        ScoreEngineService scoreEngineService) {
                this.wardrobeService = wardrobeService;
                this.aiClient = aiClient;
                this.outfitClient = outfitClient;
                this.dressCodeClient = dressCodeClient;
                this.embeddingClient = embeddingClient;
                this.colorScoreService = colorScoreService;
                this.trendScoreService = trendScoreService;
                this.tasteSimilarityService = tasteSimilarityService;
                this.scoreEngineService = scoreEngineService;

                this.databaseClient = restClientBuilder.clone()
                                .baseUrl(databaseUrl)
                                .build();
        }

        @Override
        public OutfitGenerationResponse generateOutfits(UUID userId, OutfitGenerationRequest request) {
                log.info("Outfit: generateOutfits — usuario={} ocasión={} clima={}",
                                userId, request.occasionId(), request.weatherId());

                // ── Paso 0: detectar y reparar embeddings faltantes ──────────────────
                // getCandidatesForOutfit incluye prendas con embedding null/stale;
                // ensureEmbeddingsReady repara y recarga via getWardrobeForOutfit (con filtro).
                List<ClothingEmbeddingInfo> embeddingCandidates = wardrobeService.getCandidatesForOutfit(
                                userId, request.occasionId(), request.weatherId());

                List<ClothingEmbeddingInfo> wardrobe = ensureEmbeddingsReady(userId, request, embeddingCandidates);
                log.info("Outfit: {} prendas disponibles tras Step 0", wardrobe.size());

                // ── Paso 1: validar slots mínimos ─────────────────────────────────────
                validateSlots(wardrobe);

                // ── Paso 2: resolver embedding del dress code (opcional) ──────────────
                List<Float> dresscodeEmbedding = null;
                if (request.dressCodeId() != null) {
                        DressCodeDTO dc = dressCodeClient.getDressCodeById(request.dressCodeId());
                        dresscodeEmbedding = floatArrayToList(dc.embeddingVector());
                        log.info("Outfit: Dress code {} ({})", dc.name(), request.dressCodeId());
                }

                // ── Paso 3: obtener nombre de ocasión y clima para contexto IA ───────
                CatalogDTO catalog = wardrobeService.getCatalog();
                String occasionName = catalog.occasions().stream()
                                .filter(o -> o.id().equals(request.occasionId()))
                                .map(CatalogDTO.OccasionEntry::name)
                                .findFirst()
                                .orElse("Casual");
                String weatherName = catalog.weathers().stream()
                                .filter(w -> w.id().equals(request.weatherId()))
                                .map(CatalogDTO.WeatherEntry::name)
                                .findFirst()
                                .orElse("Mild");

                // ── Paso 4: pedir candidatos a IA (con retry 3x backoff) ─────────────
                AiOutfitGenerationRequest aiRequest = new AiOutfitGenerationRequest(
                                userId.toString(),
                                wardrobe,
                                occasionName,
                                weatherName,
                                dresscodeEmbedding);
                AiOutfitGenerationResponse aiResponse = callAiWithRetry(aiRequest);

                List<ScoredOutfitCandidate> candidates = aiResponse.candidates();
                log.info("Outfit: {} candidatos recibidos de la IA", candidates.size());

                if (candidates.isEmpty()) {
                        return new OutfitGenerationResponse(List.of(), 0);
                }

                Map<UUID, ClothingEmbeddingInfo> wardrobeMap = wardrobe.stream()
                                .collect(Collectors.toMap(ClothingEmbeddingInfo::clothingId, c -> c));

                // ── Paso 5: puntuar color (paralelo) y trend (batch, 1 llamada) ──────
                List<CompletableFuture<ColorScoreResponse>> colorFutures = candidates.stream()
                                .map(c -> CompletableFuture.supplyAsync(
                                                () -> colorScoreService.score(buildColorRequest(c, wardrobeMap))))
                                .toList();

                List<List<List<Float>>> allOutfitEmbeddings = candidates.stream()
                                .map(c -> getEmbeddings(c, wardrobeMap))
                                .toList();

                CompletableFuture<List<TrendScoreResponse>> trendBatchFuture = CompletableFuture.supplyAsync(
                                () -> trendScoreService.computeBatch(allOutfitEmbeddings).scores());

                CompletableFuture.allOf(colorFutures.stream().toArray(CompletableFuture[]::new)).join();
                trendBatchFuture.join();

                List<ColorScoreResponse> colorScores = colorFutures.stream()
                                .map(CompletableFuture::join)
                                .toList();
                List<TrendScoreResponse> trendScores = trendBatchFuture.join();

                // ── Paso 6: persistir todos los candidatos para obtener sus UUIDs ─────
                List<UUID> persistedIds = new ArrayList<>();
                for (int i = 0; i < candidates.size(); i++) {
                        ScoredOutfitCandidate c = candidates.get(i);
                        List<UUID> clothingIds = c.slots().stream()
                                        .map(ScoredOutfitCandidate.CandidateSlot::clothingId)
                                        .toList();

                        OutfitResponse created = outfitClient.createOutfit(new OutfitCreateRequest(
                                        userId,
                                        clothingIds,
                                        request.dressCodeId(),
                                        request.occasionId(),
                                        request.weatherId(),
                                        null,
                                        BigDecimal.valueOf(colorScores.get(i).score()).setScale(4,
                                                        java.math.RoundingMode.HALF_UP),
                                        BigDecimal.ZERO,
                                        null,
                                        toFloatArray(c.outfitVector())));
                        persistedIds.add(created.id());
                }
                log.info("Outfit: {} candidatos persistidos", persistedIds.size());

                // ── Paso 7: calcular tasteScore en batch ──────────────────────────────
                List<TasteSimilarityResponse> tasteScores = tasteSimilarityService.computeBatch(
                                new TasteSimilarityBatchRequest(userId, persistedIds));

                Map<UUID, Double> tasteByOutfitId = tasteScores.stream()
                                .collect(Collectors.toMap(
                                                TasteSimilarityResponse::outfitId,
                                                TasteSimilarityResponse::similarity));

                // ── Paso 8: totalScore con ScoreEngine + persistir scores ─────────────
                boolean dresscodeApplies = (request.dressCodeId() != null);
                record ScoredEntry(UUID outfitId, double totalScore) {
                }
                List<ScoredEntry> scored = new ArrayList<>();

                for (int i = 0; i < candidates.size(); i++) {
                        UUID outfitId = persistedIds.get(i);
                        double taste = tasteByOutfitId.getOrDefault(outfitId, 0.0);
                        double dresscode = candidates.get(i).dresscodeScore();

                        ScoreEngineResponse engineResult = scoreEngineService.scoreFromComponents(
                                        colorScores.get(i),
                                        dresscode,
                                        dresscodeApplies,
                                        taste,
                                        trendScores.get(i));

                        outfitClient.updateOutfitScores(outfitId, new OutfitScoreUpdateRequest(
                                        BigDecimal.valueOf(taste).setScale(4, java.math.RoundingMode.HALF_UP),
                                        engineResult.totalScore()));

                        scored.add(new ScoredEntry(outfitId, engineResult.totalScore()));
                }

                // ── Paso 9: ordenar, tomar top-N y eliminar el resto ─────────────────
                scored.sort(Comparator.comparingDouble(ScoredEntry::totalScore).reversed());
                Set<UUID> topIds = scored.stream()
                                .limit(topN)
                                .map(ScoredEntry::outfitId)
                                .collect(Collectors.toCollection(LinkedHashSet::new));

                scored.stream().skip(topN).forEach(entry -> {
                        try {
                                outfitClient.deleteOutfit(entry.outfitId(), userId);
                                log.debug("Outfit: candidato descartado eliminado — {}", entry.outfitId());
                        } catch (Exception e) {
                                log.warn("Outfit: no se pudo eliminar candidato {} — {}", entry.outfitId(),
                                                e.getMessage());
                        }
                });

                List<OutfitResponse> all = outfitClient.getOutfitsByUser(userId);
                List<OutfitResponse> result = all.stream()
                                .filter(o -> topIds.contains(o.id()))
                                .sorted(Comparator.comparingInt(o -> new ArrayList<>(topIds).indexOf(o.id())))
                                .toList();

                log.info("Outfit: generación completada — {} outfits en top-{}", result.size(), topN);
                return new OutfitGenerationResponse(result, result.size());
        }

        @Override
        public List<OutfitResponse> getOutfits(UUID userId) {
                return outfitClient.getOutfitsByUser(userId);
        }

        @Override
        public OutfitDetailResponse getOutfitDetail(UUID outfitId) {
                return outfitClient.getOutfitById(outfitId);
        }

        @Override
        public OutfitResponse rateOutfit(UUID outfitId, UUID userId, OutfitRatingRequest request) {
                return outfitClient.rateOutfit(outfitId, userId, request);
        }

        private List<ClothingEmbeddingInfo> ensureEmbeddingsReady(
                        UUID userId,
                        OutfitGenerationRequest request,
                        List<ClothingEmbeddingInfo> embeddingCandidates) {

                List<UUID> missing = embeddingCandidates.stream()
                                .filter(this::hasMissingEmbedding)
                                .map(ClothingEmbeddingInfo::clothingId)
                                .toList();

                if (missing.isEmpty()) {
                        log.info("Outfit: Step 0 — todos los embeddings presentes ({} candidatos)", embeddingCandidates.size());
                        return embeddingCandidates;
                }

                log.info("Outfit: Step 0 detectó {} prendas sin embedding. Iniciando repair batch...", missing.size());
                repairMissingEmbeddings(missing);

                // Reload via for-outfit (with embedding filter) — only healthy prendas
                List<ClothingEmbeddingInfo> reloaded = wardrobeService.getWardrobeForOutfit(
                                userId, request.occasionId(), request.weatherId());

                log.info("Outfit: Step 0 completado. {} prendas disponibles tras reparación", reloaded.size());
                return reloaded;
        }

        private void repairMissingEmbeddings(List<UUID> clothingIds) {
                List<ClothingEmbeddingRequest> requests = new ArrayList<>(clothingIds.size());

                for (UUID clothingId : clothingIds) {
                        try {
                                ClothingDetailResponse detail = databaseClient.get()
                                                .uri("/internal/wardrobe/{clothingId}", clothingId)
                                                .retrieve()
                                                .body(ClothingDetailResponse.class);

                                if (detail == null || detail.categoryName() == null) {
                                        log.warn("Outfit: sin detalle para prenda {} — se omite del batch", clothingId);
                                        continue;
                                }

                                requests.add(new ClothingEmbeddingRequest(
                                                clothingId,
                                                detail.categoryName(),
                                                detail.styleName()    != null ? detail.styleName()    : "Casual",
                                                detail.colorName()    != null ? detail.colorName()    : "Neutral",
                                                detail.weatherName()  != null ? detail.weatherName()  : "Mild",
                                                detail.occasionName() != null ? detail.occasionName() : "Everyday"));
                        } catch (Exception e) {
                                log.warn("Outfit: fallo obteniendo detalle para prenda {} — {}", clothingId, e.getMessage());
                        }
                }

                if (requests.isEmpty()) {
                        log.warn("Outfit: no se pudo construir ningún request de embedding — repair abortado");
                        return;
                }

                List<ClothingEmbeddingResponse> responses;
                try {
                        responses = embeddingClient.embedClothingBatch(requests);
                } catch (Exception e) {
                        log.error("Outfit: fallo en batch embed — {}", e.getMessage());
                        return;
                }

                for (ClothingEmbeddingResponse emb : responses) {
                        if (emb == null || emb.embeddingVector() == null || emb.embeddingVector().isEmpty()) {
                                log.warn("Outfit: IA devolvió embedding vacío en batch para prenda {}",
                                                emb != null ? emb.clothingId() : "?");
                                continue;
                        }
                        try {
                                databaseClient.patch()
                                                .uri("/internal/wardrobe/{clothingId}/embedding", emb.clothingId())
                                                .body(new ClothingEmbeddingUpdateRequest(emb.clothingId(),
                                                                emb.embeddingVector()))
                                                .retrieve()
                                                .toBodilessEntity();
                                log.info("Outfit: embedding persistido para prenda {}", emb.clothingId());
                        } catch (Exception e) {
                                log.warn("Outfit: fallo persistiendo embedding para prenda {} — {}",
                                                emb.clothingId(), e.getMessage());
                        }
                }
        }

        private boolean hasMissingEmbedding(ClothingEmbeddingInfo info) {
                return info == null
                                || info.embeddingVector() == null
                                || info.embeddingVector().isEmpty();
        }

        private void validateSlots(List<ClothingEmbeddingInfo> wardrobe) {
                Set<String> slots = wardrobe.stream()
                                .map(ClothingEmbeddingInfo::slot)
                                .collect(Collectors.toSet());

                boolean hasSeparates = slots.contains("TOP") && slots.contains("BOTTOM");
                boolean hasOnepiece = slots.contains("ONEPIECE");

                if (!hasSeparates && !hasOnepiece) {
                        List<Map<String, Object>> missing = new ArrayList<>();
                        if (!slots.contains("TOP")) {
                                missing.add(Map.of("slot", "TOP", "needed", 1));
                        }
                        if (!slots.contains("BOTTOM")) {
                                missing.add(Map.of("slot", "BOTTOM", "needed", 1));
                        }

                        String detail = missing.stream()
                                        .map(m -> m.get("needed") + " " + m.get("slot"))
                                        .collect(Collectors.joining(", "));

                        throw new ResponseStatusException(
                                        HttpStatus.UNPROCESSABLE_ENTITY,
                                        "INSUFFICIENT_WARDROBE: necesitas al menos " + detail
                                                        + " para generar outfits. Sube más prendas.");
                }
        }

        private AiOutfitGenerationResponse callAiWithRetry(AiOutfitGenerationRequest request) {
                long[] delays = { 1000L, 2000L, 4000L };
                Exception last = null;

                for (int attempt = 0; attempt < 3; attempt++) {
                        try {
                                return aiClient.generateOutfits(request);
                        } catch (Exception e) {
                                last = e;
                                log.warn("Outfit: intento {}/3 fallido contra dressme-ai: {}", attempt + 1,
                                                e.getMessage());
                                if (attempt < 2) {
                                        try {
                                                Thread.sleep(delays[attempt]);
                                        } catch (InterruptedException ie) {
                                                Thread.currentThread().interrupt();
                                                break;
                                        }
                                }
                        }
                }

                log.error("Outfit: dressme-ai no disponible tras 3 intentos: {}",
                                last != null ? last.getMessage() : "?");
                throw new ResponseStatusException(
                                HttpStatus.SERVICE_UNAVAILABLE,
                                "AI_UNAVAILABLE: El servicio de IA no está disponible. Intenta más tarde.");
        }

        private ColorScoreRequest buildColorRequest(ScoredOutfitCandidate candidate,
                        Map<UUID, ClothingEmbeddingInfo> wardrobeMap) {
                List<SlotColorInput> items = candidate.slots().stream()
                                .map(slot -> {
                                        ClothingEmbeddingInfo info = wardrobeMap.get(slot.clothingId());
                                        if (info == null || info.hue() == null) {
                                                return null;
                                        }
                                        try {
                                                Slot slotEnum = Slot.valueOf(slot.slot().toUpperCase());
                                                return new SlotColorInput(slotEnum, info.hue(), info.saturation(),
                                                                info.lightness());
                                        } catch (IllegalArgumentException e) {
                                                return null;
                                        }
                                })
                                .filter(Objects::nonNull)
                                .toList();

                return new ColorScoreRequest(items.isEmpty()
                                ? List.of(new SlotColorInput(Slot.TOP, 0, 0, 50))
                                : items);
        }

        private List<List<Float>> getEmbeddings(ScoredOutfitCandidate candidate,
                        Map<UUID, ClothingEmbeddingInfo> wardrobeMap) {
                return candidate.slots().stream()
                                .map(slot -> wardrobeMap.get(slot.clothingId()))
                                .filter(info -> info != null
                                                && info.embeddingVector() != null
                                                && !info.embeddingVector().isEmpty())
                                .map(ClothingEmbeddingInfo::embeddingVector)
                                .toList();
        }

        private List<Float> floatArrayToList(float[] arr) {
                if (arr == null) {
                        return null;
                }
                List<Float> list = new ArrayList<>(arr.length);
                for (float f : arr) {
                        list.add(f);
                }
                return list;
        }

        private float[] toFloatArray(List<Float> list) {
                if (list == null) {
                        return new float[0];
                }
                float[] arr = new float[list.size()];
                for (int i = 0; i < list.size(); i++) {
                        arr[i] = list.get(i);
                }
                return arr;
        }
}