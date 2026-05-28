package com.dressme.dressme_database.service.impl;

import com.dressme.dressme_database.model.*;
import com.dressme.dressme_database.repository.*;
import com.dressme.dressme_database.schema.dto.*;
import com.dressme.dressme_database.service.ClothingService;
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
public class ClothingServiceImpl implements ClothingService {

    private final ClothingRepository         clothingRepository;
    private final ClothingAiAuditRepository  auditRepository;
    private final ClothingCategoryRepository categoryRepository;
    private final StyleRepository            styleRepository;
    private final ColorRepository            colorRepository;
    private final OccasionRepository         occasionRepository;
    private final WeatherRepository          weatherRepository;
    private final UserRepository             userRepository;

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
        weatherRepository.findById(request.predictedWeatherId())
                .ifPresent(audit::setPredictedWeather);
        occasionRepository.findById(request.predictedOccasionId())
                .ifPresent(audit::setPredictedOccasion);

        auditRepository.save(audit);
        log.info("ClothingService: Audit aplicado — prenda {} procesada", clothing.getId());
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
    public ClothingDetailResponse updateClothing(UUID clothingId, UUID userId,
                                                  ClothingUpdateRequest request) {
        log.info("ClothingService: Corrección manual — prenda {} por usuario {}",
                clothingId, userId);

        // Verificar ownership antes de cualquier write
        if (!clothingRepository.existsByIdAndUserId(clothingId, userId)) {
            throw new RuntimeException(
                    "Prenda no encontrada o no pertenece al usuario: " + clothingId);
        }

        Clothing clothing = clothingRepository.findById(clothingId)
                .orElseThrow(() -> new RuntimeException(
                        "Prenda no encontrada: " + clothingId));

        // Resolver todas las referencias del catálogo
        ClothingCategory category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new RuntimeException(
                        "Categoría no encontrada: " + request.categoryId()));
        Style style = styleRepository.findById(request.styleId())
                .orElseThrow(() -> new RuntimeException(
                        "Estilo no encontrado: " + request.styleId()));
        Color color = colorRepository.findById(request.colorId())
                .orElseThrow(() -> new RuntimeException(
                        "Color no encontrado: " + request.colorId()));
        Weather weather = weatherRepository.findById(request.weatherId())
                .orElseThrow(() -> new RuntimeException(
                        "Clima no encontrado: " + request.weatherId()));
        Occasion occasion = occasionRepository.findById(request.occasionId())
                .orElseThrow(() -> new RuntimeException(
                        "Ocasión no encontrada: " + request.occasionId()));

        // Actualizar categoría en tbl_clothes
        clothing.setCategory(category);
        clothingRepository.save(clothing);

        // Actualizar audit — crear si aún no existe (prenda no procesada por IA
        // pero el usuario quiere asignar manualmente)
        ClothingAiAudit audit = auditRepository
                .findByClothingId(clothingId)
                .orElse(ClothingAiAudit.builder().clothing(clothing).build());

        audit.setPredictedCategory(category);
        audit.setPredictedStyle(style);
        audit.setPredictedColor(color);
        audit.setPredictedWeather(weather);
        audit.setPredictedOccasion(occasion);
        audit.setWasCorrected(true); // ← señal clave para el scoring engine

        // Si no había audit previo, marcar la prenda como procesada
        // (el usuario la caracterizó manualmente)
        if (!clothing.isProcessed()) {
            clothing.setProcessed(true);
            clothingRepository.save(clothing);
        }

        auditRepository.save(audit);
        log.info("ClothingService: Prenda {} actualizada manualmente — was_corrected=true",
                clothingId);

        return toDetail(clothing, audit);
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
                // Clima
                audit != null && audit.getPredictedWeather()  != null ? audit.getPredictedWeather().getId()   : null,
                audit != null && audit.getPredictedWeather()  != null ? audit.getPredictedWeather().getName() : null,
                // Ocasión
                audit != null && audit.getPredictedOccasion() != null ? audit.getPredictedOccasion().getId()   : null,
                audit != null && audit.getPredictedOccasion() != null ? audit.getPredictedOccasion().getName() : null,
                // Audit metadata
                audit != null ? audit.getConfidenceScore() : null,
                audit != null && audit.isWasCorrected(),
                audit != null ? audit.getAiProvider() : null
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

    private float hueToRgb(float p, float q, float t) {
        if (t < 0) t += 1;
        if (t > 1) t -= 1;
        if (t < 1f / 6)  return p + (q - p) * 6 * t;
        if (t < 1f / 2)  return q;
        if (t < 2f / 3)  return p + (q - p) * (2f / 3 - t) * 6;
        return p;
    }
}