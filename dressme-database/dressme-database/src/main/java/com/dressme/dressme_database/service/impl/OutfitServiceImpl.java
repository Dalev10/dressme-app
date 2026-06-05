package com.dressme.dressme_database.service.impl;

import com.dressme.dressme_database.model.*;
import com.dressme.dressme_database.repository.*;
import com.dressme.dressme_database.schema.dto.*;
import com.dressme.dressme_database.service.OutfitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutfitServiceImpl implements OutfitService {

    private final OutfitRepository          outfitRepository;
    private final OutfitAiAuditRepository   auditRepository;
    private final ClothingOutfitRepository  clothingOutfitRepository;
    private final ClothingRepository        clothingRepository;
    private final ClothingAiAuditRepository clothingAuditRepository;
    private final UserRepository            userRepository;
    private final OccasionRepository        occasionRepository;
    private final WeatherRepository         weatherRepository;
    private final DressCodeRepository       dressCodeRepository;

    // ── Dress code compatibility ─────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public OutfitDressCodeResponse getDressCode(UUID outfitId) {
        Outfit outfit = outfitRepository.findById(outfitId)
                .orElseThrow(() ->
                        new RuntimeException("Outfit no encontrado con ID: " + outfitId));

        UUID dressCodeId = outfit.getDressCode() != null
                ? outfit.getDressCode().getId()
                : null;

        log.info("OutfitService: Outfit {} tiene dressCode {}", outfitId, dressCodeId);

        return new OutfitDressCodeResponse(outfitId, dressCodeId);
    }

    // ── Crear ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public OutfitResponse createOutfit(OutfitCreateRequest request) {
        log.info("OutfitService: Creando outfit para usuario {} con {} prendas",
                request.userId(), request.clothingIds().size());

        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new RuntimeException(
                        "Usuario no encontrado: " + request.userId()));

        DressCode dressCode = request.dressCodeId() != null
                ? dressCodeRepository.findById(request.dressCodeId()).orElse(null)
                : null;

        Occasion occasion = request.occasionId() != null
                ? occasionRepository.findById(request.occasionId()).orElse(null)
                : null;

        Weather weather = request.weatherId() != null
                ? weatherRepository.findById(request.weatherId()).orElse(null)
                : null;

        // 1. Persistir outfit
        Outfit outfit = Outfit.builder()
                .user(user)
                .dressCode(dressCode)
                .occasion(occasion)
                .weather(weather)
                .name(request.name())
                .isAiProcessed(true)
                .build();

        outfit = outfitRepository.save(outfit);

        // 2. Asociaciones outfit-prendas
        List<Clothing> clothes =
                clothingRepository.findAllById(request.clothingIds());

        if (clothes.size() != request.clothingIds().size()) {
            log.warn(
                    "OutfitService: Algunas prendas no encontradas. " +
                    "Solicitadas: {}, encontradas: {}",
                    request.clothingIds().size(),
                    clothes.size()
            );
        }

        final Outfit savedOutfit = outfit;

        List<ClothingOutfit> associations = clothes.stream()
                .map(c -> ClothingOutfit.builder()
                        .clothing(c)
                        .outfit(savedOutfit)
                        .build())
                .collect(Collectors.toList());

        clothingOutfitRepository.saveAll(associations);

        // 3. Audit IA
        OutfitAiAudit audit = OutfitAiAudit.builder()
                .outfit(savedOutfit)
                .matchScore(request.matchScore())
                .affinityScore(request.affinityScore())
                .totalScore(request.totalScore())
                .outfitVector(request.outfitVector())
                .build();

        auditRepository.save(audit);

        log.info(
                "OutfitService: Outfit {} creado — matchScore={}, affinityScore={}, totalScore={}",
                savedOutfit.getId(),
                request.matchScore(),
                request.affinityScore(),
                request.totalScore()
        );

        return toSummary(savedOutfit, clothes, audit);
    }

    // ── Actualizar scores ────────────────────────────────────────────────────

    @Override
    @Transactional
    public void updateScores(UUID outfitId, OutfitScoreUpdateRequest request) {
        OutfitAiAudit audit = auditRepository.findByOutfitId(outfitId)
                .orElseThrow(() -> new RuntimeException(
                        "OutfitAiAudit no encontrado para outfit: " + outfitId));
        audit.setAffinityScore(request.affinityScore());
        audit.setTotalScore(request.totalScore());
        auditRepository.save(audit);
        log.info("OutfitService: Scores actualizados — outfit={} affinity={} total={}",
                outfitId, request.affinityScore(), request.totalScore());
    }

    // ── Listar ───────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<OutfitResponse> getOutfitsByUser(UUID userId) {
        return outfitRepository.findTopByUserIdOrderByAffinityScore(userId)
                .stream()
                .map(outfit -> {
                    List<Clothing> clothes =
                            clothingOutfitRepository.findByOutfitId(outfit.getId())
                                    .stream()
                                    .map(ClothingOutfit::getClothing)
                                    .collect(Collectors.toList());

                    OutfitAiAudit audit =
                            auditRepository.findByOutfitId(outfit.getId())
                                    .orElse(null);

                    return toSummary(outfit, clothes, audit);
                })
                .collect(Collectors.toList());
    }

    // ── Detalle ──────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public OutfitDetailResponse getOutfitDetail(UUID outfitId) {
        Outfit outfit = outfitRepository.findById(outfitId)
                .orElseThrow(() ->
                        new RuntimeException("Outfit no encontrado: " + outfitId));

        List<Clothing> clothes =
                clothingOutfitRepository.findByOutfitId(outfitId)
                        .stream()
                        .map(ClothingOutfit::getClothing)
                        .collect(Collectors.toList());

        OutfitAiAudit audit =
                auditRepository.findByOutfitId(outfitId).orElse(null);

        return toDetail(outfit, clothes, audit);
    }

    // ── Rating ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public OutfitResponse rateOutfit(
            UUID outfitId,
            UUID userId,
            OutfitRatingRequest request
    ) {
        log.info(
                "OutfitService: Rating {}/5 para outfit {} del usuario {}",
                request.rating(),
                outfitId,
                userId
        );

        if (!outfitRepository.existsByIdAndUserId(outfitId, userId)) {
            throw new RuntimeException(
                    "Outfit no encontrado o no pertenece al usuario: " + outfitId
            );
        }

        Outfit outfit = outfitRepository.findById(outfitId)
                .orElseThrow(() ->
                        new RuntimeException("Outfit no encontrado: " + outfitId));

        outfit.setRating(request.rating());
        outfitRepository.save(outfit);

        List<Clothing> clothes =
                clothingOutfitRepository.findByOutfitId(outfitId)
                        .stream()
                        .map(ClothingOutfit::getClothing)
                        .collect(Collectors.toList());

        OutfitAiAudit audit =
                auditRepository.findByOutfitId(outfitId).orElse(null);

        return toSummary(outfit, clothes, audit);
    }

    // ── Eliminar ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void deleteOutfit(UUID outfitId, UUID userId) {
        log.info(
                "OutfitService: Eliminando outfit {} del usuario {}",
                outfitId,
                userId
        );

        if (!outfitRepository.existsByIdAndUserId(outfitId, userId)) {
            throw new RuntimeException(
                    "Outfit no encontrado o no pertenece al usuario: " + outfitId
            );
        }

        auditRepository.findByOutfitId(outfitId)
                .ifPresent(auditRepository::delete);

        clothingOutfitRepository.deleteByOutfitId(outfitId);
        outfitRepository.deleteById(outfitId);

        log.info("OutfitService: Outfit {} eliminado", outfitId);
    }

    // ── Mappers ──────────────────────────────────────────────────────────────

    private OutfitResponse toSummary(
            Outfit o,
            List<Clothing> clothes,
            OutfitAiAudit audit
    ) {
        return new OutfitResponse(
                o.getId(),
                o.getUser().getId(),
                o.getName(),
                clothes.stream()
                        .map(Clothing::getImageUrl)
                        .collect(Collectors.toList()),
                clothes.stream()
                        .map(Clothing::getId)
                        .collect(Collectors.toList()),
                o.getOccasion() != null ? o.getOccasion().getId() : null,
                o.getOccasion() != null ? o.getOccasion().getName() : null,
                o.getWeather() != null ? o.getWeather().getId() : null,
                o.getWeather() != null ? o.getWeather().getName() : null,
                audit != null ? audit.getMatchScore() : null,
                audit != null ? audit.getAffinityScore() : null,
                audit != null ? audit.getTotalScore() : null,
                o.getRating(),
                o.isAiProcessed(),
                o.getCreatedAt()
        );
    }

    private OutfitDetailResponse toDetail(
            Outfit o,
            List<Clothing> clothes,
            OutfitAiAudit audit
    ) {
        List<OutfitDetailResponse.ClothingSlot> slots = clothes.stream()
                .map(c -> {
                    ClothingAiAudit cAudit =
                            clothingAuditRepository
                                    .findByClothingId(c.getId())
                                    .orElse(null);

                    String colorName =
                            cAudit != null &&
                            cAudit.getPredictedColor() != null
                                    ? cAudit.getPredictedColor().getName()
                                    : null;

                    String colorHex =
                            cAudit != null &&
                            cAudit.getPredictedColor() != null
                                    ? hslToHex(cAudit.getPredictedColor())
                                    : null;

                    return new OutfitDetailResponse.ClothingSlot(
                            c.getId(),
                            c.getImageUrl(),
                            c.getCategory() != null
                                    ? c.getCategory().getName()
                                    : null,
                            colorName,
                            colorHex
                    );
                })
                .collect(Collectors.toList());

        return new OutfitDetailResponse(
                o.getId(),
                o.getUser().getId(),
                o.getName(),
                o.getRating(),
                o.isAiProcessed(),
                o.getCreatedAt(),
                o.getDressCode() != null ? o.getDressCode().getId() : null,
                o.getDressCode() != null ? o.getDressCode().getName() : null,
                o.getOccasion() != null ? o.getOccasion().getId() : null,
                o.getOccasion() != null ? o.getOccasion().getName() : null,
                o.getWeather() != null ? o.getWeather().getId() : null,
                o.getWeather() != null ? o.getWeather().getName() : null,
                slots,
                audit != null ? audit.getMatchScore() : null,
                audit != null ? audit.getAffinityScore() : null
        );
    }

    // ── Utilidades ───────────────────────────────────────────────────────────

    private String hslToHex(Color color) {
        float h = color.getHue() / 360f;
        float s = color.getSaturation() / 100f;
        float l = color.getLightness() / 100f;

        float q = l < 0.5f ? l * (1 + s) : l + s - l * s;
        float p = 2 * l - q;

        int r = Math.round(hueToRgb(p, q, h + 1f / 3) * 255);
        int g = Math.round(hueToRgb(p, q, h) * 255);
        int b = Math.round(hueToRgb(p, q, h - 1f / 3) * 255);

        return String.format("#%02X%02X%02X", r, g, b);
    }

    private float hueToRgb(float p, float q, float t) {
        if (t < 0) t += 1;
        if (t > 1) t -= 1;
        if (t < 1f / 6) return p + (q - p) * 6 * t;
        if (t < 1f / 2) return q;
        if (t < 2f / 3) return p + (q - p) * (2f / 3 - t) * 6;
        return p;
    }
}