package com.dressme.dressme_database.service.impl;

import com.dressme.dressme_database.exception.CategoryTypeMismatchException;
import com.dressme.dressme_database.model.*;
import com.dressme.dressme_database.repository.*;
import com.dressme.dressme_database.schema.dto.*;
import com.dressme.dressme_database.service.ClothingService;
import com.dressme.dressme_database.util.CategorySlotMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClothingServiceImpl implements ClothingService {

    private final ClothingRepository          clothingRepository;
    private final ClothingAiAuditRepository   auditRepository;
    private final ClothingCategoryRepository  categoryRepository;
    private final StyleRepository             styleRepository;
    private final ColorRepository             colorRepository;
    private final OccasionRepository          occasionRepository;
    private final WeatherRepository           weatherRepository;
    private final UserRepository              userRepository;
    private final ClothingOccasionRepository  clothingOccasionRepository;
    private final ClothingWeatherRepository   clothingWeatherRepository;

    // ── Paso 1: Registro inicial ──────────────────────────────────────────────

    @Override
    @Transactional
    public ClothingItemResponse createClothing(ClothingCreateRequest request) {
        log.info("ClothingService: Registrando prenda para usuario {}", request.userId());

        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new RuntimeException(
                        "Usuario no encontrado: " + request.userId()));

        Clothing clothing = Clothing.builder()
                .user(user)
                .imageUrl(request.imageUrl())
                .isProcessed(false)
                .build();

        clothing = clothingRepository.save(clothing);
        log.info("ClothingService: Prenda {} creada — pendiente de análisis IA", clothing.getId());
        return toSummary(clothing);
    }

    // ── Paso 2: Aplicar resultado Vision+Gemini ───────────────────────────────

    @Override
    @Transactional
    public ClothingItemResponse applyAiAudit(AuditUpdateRequest request) {
        log.info("ClothingService: Aplicando audit IA para prenda {}", request.clothingId());

        Clothing clothing = clothingRepository.findById(request.clothingId())
                .orElseThrow(() -> new RuntimeException(
                        "Prenda no encontrada: " + request.clothingId()));

        ClothingCategory category = categoryRepository.findById(request.predictedCategoryId())
                .orElseThrow(() -> new RuntimeException(
                        "Categoría no encontrada: " + request.predictedCategoryId()));

        // Actualizar la prenda
        clothing.setCategory(category);
        clothing.setProcessed(true);
        clothingRepository.save(clothing);

        // Crear o actualizar el registro de auditoría IA
        ClothingAiAudit audit = auditRepository
                .findByClothingId(clothing.getId())
                .orElse(ClothingAiAudit.builder().clothing(clothing).build());

        audit.setPredictedCategory(category);
        audit.setConfidenceScore(request.confidenceScore());
        audit.setAiProvider(request.aiProvider());
        audit.setWasCorrected(false);

        styleRepository.findById(request.predictedStyleId())
                .ifPresent(audit::setPredictedStyle);
        colorRepository.findById(request.predictedColorId())
                .ifPresent(audit::setPredictedColor);

        // Persistir valores HSL detectados exactamente por Gemini Vision
        audit.setDetectedHue(request.detectedHue());
        audit.setDetectedSaturation(request.detectedSaturation());
        audit.setDetectedLightness(request.detectedLightness());

        // Primary weather/occasion on the audit (first of the list — used by embedding step)
        if (request.predictedWeatherIds() != null && !request.predictedWeatherIds().isEmpty()) {
            weatherRepository.findById(request.predictedWeatherIds().get(0))
                    .ifPresent(audit::setPredictedWeather);
        }
        if (request.predictedOccasionIds() != null && !request.predictedOccasionIds().isEmpty()) {
            occasionRepository.findById(request.predictedOccasionIds().get(0))
                    .ifPresent(audit::setPredictedOccasion);
        }

        auditRepository.save(audit);

        // Populate M:N join tables — delete previous entries then insert all predicted values
        clothingOccasionRepository.deleteByClothingId(clothing.getId());
        for (UUID occasionId : request.predictedOccasionIds()) {
            occasionRepository.findById(occasionId).ifPresent(occasion ->
                clothingOccasionRepository.save(
                    ClothingOccasion.builder().clothing(clothing).occasion(occasion).build()
                )
            );
        }

        clothingWeatherRepository.deleteByClothingId(clothing.getId());
        for (UUID weatherId : request.predictedWeatherIds()) {
            weatherRepository.findById(weatherId).ifPresent(weather ->
                clothingWeatherRepository.save(
                    ClothingWeather.builder().clothing(clothing).weather(weather).build()
                )
            );
        }

        log.info("ClothingService: Audit aplicado — prenda {} procesada ({} ocasiones, {} climas)",
                clothing.getId(),
                request.predictedOccasionIds().size(),
                request.predictedWeatherIds().size());
        return toSummary(clothing);
    }

    // ── Get list (resumido) ───────────────────────────────────────────────────

    @Override
    public List<ClothingItemResponse> getWardrobeByUser(UUID userId) {
        return clothingRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    // ── Get by ID (detalle compuesto) ─────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ClothingDetailResponse getClothingDetail(UUID clothingId) {
        Clothing clothing = clothingRepository.findById(clothingId)
                .orElseThrow(() -> new RuntimeException(
                        "Prenda no encontrada: " + clothingId));

        ClothingAiAudit audit = auditRepository
                .findByClothingId(clothingId)
                .orElse(null);

        return toDetail(clothing, audit);
    }

    // ── Update (corrección manual) ────────────────────────────────────────────

    @Override
    @Transactional
    public ClothingDetailResponse updateClothing(
                UUID clothingId,
                UUID userId,
                ClothingUpdateRequest request) {

        log.info(
                "ClothingService: Corrección manual — prenda {} por usuario {}",
                clothingId,
                userId
        );

        // Verificar ownership antes de cualquier write
        if (!clothingRepository.existsByIdAndUserId(clothingId, userId)) {
            throw new RuntimeException(
                    "Prenda no encontrada o no pertenece al usuario: " + clothingId);
        }

        Clothing clothing = clothingRepository.findById(clothingId)
                .orElseThrow(() -> new RuntimeException(
                        "Prenda no encontrada: " + clothingId));

        ClothingCategory type = categoryRepository.findById(request.typeId())
                .orElseThrow(() -> new RuntimeException(
                        "Tipo no encontrado: " + request.typeId()));

        if (type.getParent() != null) {
            throw new CategoryTypeMismatchException(
                    "El ID de tipo "
                        + request.typeId()
                        + " no corresponde a una categoría raíz"
                );
        }

        ClothingCategory category =
                categoryRepository.findByIdAndParentIdAndIsActiveTrue(
                        request.categoryId(),
                        request.typeId()
                )
                .orElseThrow(() ->
                        new CategoryTypeMismatchException(
                                "La categoria"
                                + request.categoryId()
                                + " no pertenece al tipo"
                                + request.typeId()
                        ));

        Style style = styleRepository.findById(request.styleId())
                .orElseThrow(() -> new RuntimeException(
                        "Estilo no encontrado: " + request.styleId()));

        // Actualizar categoría en tbl_clothes
        clothing.setCategory(category);
        // Invariante: vector corrupto eliminado
        clothing.setEmbeddingVector(null);
        // Señal para re-vectorización 
        clothing.setEmbeddingStale(true);

        // Actualizar audit — crear si aún no existe (prenda no procesada por IA
        // pero el usuario quiere asignar manualmente)
        ClothingAiAudit audit = auditRepository
                .findByClothingId(clothingId)
                .orElse(ClothingAiAudit.builder().clothing(clothing).build());

        /*
        * Reservado para ClothingCorrectionEvent.
        *
        * Permitirá capturar el cambio semántico
        * realizado por el usuario para futuros
        * procesos de feedback, analytics,
        * active learning y reentrenamiento IA.
        */


        audit.setPredictedCategory(category);
        audit.setPredictedStyle(style);
        audit.setWasCorrected(true); // ← señal clave para el scoring engine

        // Si no había audit previo, marcar la prenda como procesada
        // (el usuario la caracterizó manualmente)
        if (!clothing.isProcessed()) {
            clothing.setProcessed(true);
        }

        clothingRepository.save(clothing);
        auditRepository.save(audit);

        log.info("ClothingService: Prenda {} actualizada — embedding was_corrected=true",
                clothingId);

        return toDetail(clothing, audit);
    }

    @Override
    @Transactional(readOnly = true)
    public WardrobeEditCatalogDTO getEditCatalog() {
        log.info("ClothingService: Obteniendo catálogo para edición de guardarropa");

        List<WardrobeEditCatalogDTO.CategoryEntry> categories =
                categoryRepository
                        .findByIsActiveTrueOrderByNameAsc()
                        .stream()
                        .map(category ->
                                new WardrobeEditCatalogDTO.CategoryEntry(
                                        category.getId(),
                                        category.getName(),
                                        category.getParent() != null
                                                ? category.getParent().getId()
                                                : null
                                        )
                                )
                                .toList();
        List<WardrobeEditCatalogDTO.StyleEntry> styles =
                styleRepository
                        .findByIsActiveTrueOrderByNameAsc()
                        .stream()
                        .map(style ->
                                new WardrobeEditCatalogDTO.StyleEntry(
                                        style.getId(),
                                        style.getName()
                                        )
                                )
                        .toList();

        return new WardrobeEditCatalogDTO(
                categories,
                styles
        );
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void deleteClothing(UUID clothingId, UUID userId) {
        if (!clothingRepository.existsByIdAndUserId(clothingId, userId)) {
            throw new RuntimeException(
                    "Prenda no encontrada o no pertenece al usuario: " + clothingId);
        }
        auditRepository.findByClothingId(clothingId)
                .ifPresent(auditRepository::delete);
        clothingRepository.deleteById(clothingId);
        log.info("ClothingService: Prenda {} eliminada", clothingId);
    }

    // ── Catálogo ──────────────────────────────────────────────────────────────

    @Override
    public CatalogDTO getCatalog() {
        List<CatalogDTO.CategoryEntry> categories = categoryRepository
                .findByIsActiveTrueOrderByNameAsc().stream()
                .map(c -> new CatalogDTO.CategoryEntry(
                        c.getId(), c.getName(),
                        c.getParent() != null ? c.getParent().getId() : null))
                .collect(Collectors.toList());

        List<CatalogDTO.StyleEntry> styles = styleRepository
                .findByIsActiveTrueOrderByNameAsc().stream()
                .map(s -> new CatalogDTO.StyleEntry(s.getId(), s.getName()))
                .collect(Collectors.toList());

        List<CatalogDTO.ColorEntry> colors = colorRepository
                .findAllByOrderByNameAsc().stream()
                .map(c -> new CatalogDTO.ColorEntry(
                        c.getId(), c.getName(),
                        c.getHue(), c.getSaturation(), c.getLightness()))
                .collect(Collectors.toList());

        List<CatalogDTO.OccasionEntry> occasions = occasionRepository
                .findByIsActiveTrueOrderByNameAsc().stream()
                .map(o -> new CatalogDTO.OccasionEntry(o.getId(), o.getName()))
                .collect(Collectors.toList());

        List<CatalogDTO.WeatherEntry> weathers = weatherRepository
                .findByIsActiveTrueOrderByNameAsc().stream()
                .map(w -> new CatalogDTO.WeatherEntry(w.getId(), w.getName()))
                .collect(Collectors.toList());

        return new CatalogDTO(categories, styles, colors, occasions, weathers);
    }

    // ── Outfit Generation ─────────────────────────────────────────────────────

    /**
     * Paso 1 de 2 — query de metadatos escalares via proyección JPA.
     * Paso 2 de 2 — hidratación del embeddingVector desde entidades Clothing.
     *
     * El split en dos pasos es deliberado: el tipo vector(1536) de pgvector
     * no se serializa de forma fiable como campo de proyección plana en todos
     * los drivers JDBC. Cargar la entidad Clothing completa (con el
     * @JdbcTypeCode(SqlTypes.VECTOR)) garantiza la deserialización correcta.
     */
    @Override
    @Transactional(readOnly = true)
    public List<ClothingEmbeddingDTO> getClothingForOutfit(
            UUID userId, UUID occasionId, UUID weatherId) {

        log.info("ClothingService: Cargando guardarropa para outfit generation — " +
                 "userId={}, occasionId={}, weatherId={}", userId, occasionId, weatherId);

        // Validar que el usuario existe (fail-fast antes del query costoso)
        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Usuario no encontrado: " + userId);
        }

        // 1. Metadatos escalares via proyección JPA (sin embedding_vector)
        List<ClothingWithEmbeddingProjection> projections =
                clothingRepository.findForOutfitGeneration(userId, occasionId, weatherId);

        if (projections.isEmpty()) {
            log.info("ClothingService: Sin prendas aptas para userId={} occasionId={} weatherId={}",
                     userId, occasionId, weatherId);
            return List.of();
        }

        // 2. IDs para hidratar los embeddings en un segundo query
        List<UUID> ids = projections.stream()
                .map(ClothingWithEmbeddingProjection::getClothingId)
                .collect(Collectors.toList());

        // 3. Entidades Clothing con el embedding_vector deserializado correctamente
        List<Clothing> clothingWithEmbeddings = clothingRepository.findEmbeddingsByIds(ids);

        // 4. Mapa clothingId → embeddingVector para lookup O(1)
        Map<UUID, float[]> embeddingMap = clothingWithEmbeddings.stream()
                .collect(Collectors.toMap(
                        Clothing::getId,
                        Clothing::getEmbeddingVector
                ));

        // 5. Combinar proyección + embedding en el DTO final
        List<ClothingEmbeddingDTO> result = new ArrayList<>(projections.size());
        for (ClothingWithEmbeddingProjection p : projections) {
            float[] rawVector = embeddingMap.get(p.getClothingId());

            if (rawVector == null) {
                // No debería ocurrir con el query correcto, pero es mejor
                // omitir la prenda que lanzar NPE en mitad del flujo.
                log.warn("ClothingService: embeddingVector null inesperado para clothingId={}",
                         p.getClothingId());
                continue;
            }

            // Resolver el slot canónico (TOP/BOTTOM/OUTERWEAR/FOOTWEAR/ONEPIECE).
            // null = accesorio o categoría no apta para outfit → se omite.
            String slot = CategorySlotMapper.toSlot(p.getCategoryName(), p.getParentName());
            if (slot == null) {
                log.debug("ClothingService: prenda {} omitida — categoría '{}' (padre '{}') " +
                          "no mapea a un slot de outfit",
                          p.getClothingId(), p.getCategoryName(), p.getParentName());
                continue;
            }

            // Convertir float[] → List<Float> para el contrato del DTO
            List<Float> embeddingList = new ArrayList<>(rawVector.length);
            for (float v : rawVector) {
                embeddingList.add(v);
            }

            result.add(new ClothingEmbeddingDTO(
                    p.getClothingId(),
                    slot,
                    p.getDetectedHue(),
                    p.getDetectedSaturation(),
                    p.getDetectedLightness(),
                    embeddingList,
                    p.getOccasionName(),
                    p.getWeatherName()
            ));
        }

        log.info("ClothingService: {} prendas retornadas para outfit generation", result.size());
        return result;
    }

    // ── Step 0 candidates (includes null/stale embeddings) ───────────────────

    @Override
    @Transactional(readOnly = true)
    public List<ClothingEmbeddingDTO> getCandidatesForOutfit(
            UUID userId, UUID occasionId, UUID weatherId) {

        log.info("ClothingService: Cargando candidatos para Step 0 (incl. sin embedding) — " +
                 "userId={}, occasionId={}, weatherId={}", userId, occasionId, weatherId);

        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Usuario no encontrado: " + userId);
        }

        List<ClothingWithEmbeddingProjection> projections =
                clothingRepository.findCandidatesForOutfitGeneration(userId, occasionId, weatherId);

        if (projections.isEmpty()) {
            log.info("ClothingService: Sin candidatos para userId={}", userId);
            return List.of();
        }

        List<UUID> ids = projections.stream()
                .map(ClothingWithEmbeddingProjection::getClothingId)
                .collect(Collectors.toList());

        List<Clothing> clothingEntities = clothingRepository.findEmbeddingsByIds(ids);

        // Use explicit HashMap to allow null embeddingVector values
        Map<UUID, float[]> embeddingMap = new java.util.HashMap<>();
        for (Clothing c : clothingEntities) {
            embeddingMap.put(c.getId(), c.getEmbeddingVector());
        }

        List<ClothingEmbeddingDTO> result = new ArrayList<>(projections.size());
        for (ClothingWithEmbeddingProjection p : projections) {
            String slot = CategorySlotMapper.toSlot(p.getCategoryName(), p.getParentName());
            if (slot == null) {
                log.debug("ClothingService: candidato {} omitido — categoría '{}' no mapea a slot",
                          p.getClothingId(), p.getCategoryName());
                continue;
            }

            float[] rawVector = embeddingMap.get(p.getClothingId());
            List<Float> embeddingList = null;
            if (rawVector != null) {
                embeddingList = new ArrayList<>(rawVector.length);
                for (float v : rawVector) {
                    embeddingList.add(v);
                }
            }

            result.add(new ClothingEmbeddingDTO(
                    p.getClothingId(),
                    slot,
                    p.getDetectedHue(),
                    p.getDetectedSaturation(),
                    p.getDetectedLightness(),
                    embeddingList,
                    p.getOccasionName(),
                    p.getWeatherName()
            ));
        }

        long nullCount = result.stream().filter(d -> d.embeddingVector() == null).count();
        log.info("ClothingService: {} candidatos retornados ({} sin embedding)", result.size(), nullCount);
        return result;
    }

    // ── Mappers privados ──────────────────────────────────────────────────────

    /** Vista resumida — usada en el listado del guardarropa. */
    private ClothingItemResponse toSummary(Clothing c) {
        return new ClothingItemResponse(
                c.getId(),
                c.getUser().getId(),
                c.getImageUrl(),
                c.getCategory() != null ? c.getCategory().getId()   : null,
                c.getCategory() != null ? c.getCategory().getName() : null,
                c.isProcessed(),
                c.getCreatedAt()
        );
    }

    /** Vista detallada — une prenda + audit. Todos los campos del audit son nullable. */
    private ClothingDetailResponse toDetail(Clothing c, ClothingAiAudit audit) {
        return new ClothingDetailResponse(
                c.getId(),
                c.getUser().getId(),
                c.getImageUrl(),
                c.getCreatedAt(),
                c.isProcessed(),
                // Categoría (de tbl_clothes)
                c.getCategory() != null ? c.getCategory().getId()   : null,
                c.getCategory() != null ? c.getCategory().getName() : null,
                // Estilo
                audit != null && audit.getPredictedStyle()    != null ? audit.getPredictedStyle().getId()      : null,
                audit != null && audit.getPredictedStyle()    != null ? audit.getPredictedStyle().getName()    : null,
                // Color
                audit != null && audit.getPredictedColor()    != null ? audit.getPredictedColor().getId()      : null,
                audit != null && audit.getPredictedColor()    != null ? audit.getPredictedColor().getName()    : null,
                audit != null && audit.getPredictedColor()    != null ? hslToHex(audit.getPredictedColor())    : null,
                // Valores HSL detectados por Gemini Vision
                audit != null ? audit.getDetectedHue()        : null,
                audit != null ? audit.getDetectedSaturation() : null,
                audit != null ? audit.getDetectedLightness()  : null,
                // Clima
                audit != null && audit.getPredictedWeather()  != null ? audit.getPredictedWeather().getId()   : null,
                audit != null && audit.getPredictedWeather()  != null ? audit.getPredictedWeather().getName() : null,
                // Ocasión
                audit != null && audit.getPredictedOccasion() != null ? audit.getPredictedOccasion().getId()   : null,
                audit != null && audit.getPredictedOccasion() != null ? audit.getPredictedOccasion().getName() : null,
                // Audit metadata
                audit != null ? audit.getConfidenceScore() : null,
                audit != null && audit.isWasCorrected(),
                audit != null ? audit.getAiProvider() : null,
                c.isEmbeddingStale()
        );
    }

    /**
     * Convierte un Color (HSL del catálogo) a su representación hex aproximada.
     * Usado en el frontend para mostrar el chip de color sin una tabla de imágenes.
     */
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

    @Override
    @Transactional
    public void saveEmbedding(ClothingEmbeddingUpdateRequest request) {
        Clothing clothing = clothingRepository.findById(request.clothingId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Prenda no encontrada: " + request.clothingId()));

        float[] vector = new float[request.embeddingVector().size()];
        for (int i = 0; i < request.embeddingVector().size(); i++) {
            vector[i] = request.embeddingVector().get(i);
        }

        clothing.setEmbeddingVector(vector);
        clothing.setEmbeddingStale(false);
        clothingRepository.save(clothing);

        log.info("ClothingService: Embedding guardado para prenda {} ({} dims)",
                request.clothingId(), vector.length);
    }

    private float hueToRgb(float p, float q, float t) {
        if (t < 0) t += 1;
        if (t > 1) t -= 1;
        if (t < 1f / 6)  return p + (q - p) * 6 * t;
        if (t < 1f / 2)  return q;
        if (t < 2f / 3)  return p + (q - p) * (2f / 3 - t) * 6;
        return p;
    }
}