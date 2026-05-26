package com.dressme.dressme_database.service.impl;

import com.dressme.dressme_database.model.*;
import com.dressme.dressme_database.repository.*;
import com.dressme.dressme_database.schema.dto.*;
import com.dressme.dressme_database.repository.StyleRepository;
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

    private final ClothingRepository clothingRepository;
    private final ClothingAiAuditRepository clothingAiAuditRepository;
    private final ClothingCategoryRepository categoryRepository;
    private final OccasionRepository occasionRepository;
    private final WeatherRepository weatherRepository;
    private final StyleRepository styleRepository;
    private final UserRepository userRepository;

    // ── Paso 1: Registro inicial ──────────────────────────────────────────────

    @Override
    @Transactional
    public ClothingItemResponse createClothing(ClothingCreateRequest request) {
        log.info("ClothingService: Registrando nueva prenda para usuario {}", request.userId());

        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new RuntimeException(
                        "Usuario no encontrado con ID: " + request.userId()));

        Clothing clothing = Clothing.builder()
                .user(user)
                .imageUrl(request.imageUrl())
                .isProcessed(false)
                // category = null intencionalmente (nullable por diseño)
                .build();

        clothing = clothingRepository.save(clothing);
        log.info("ClothingService: Prenda creada con ID {} — pendiente de análisis IA",
                clothing.getId());

        return toResponse(clothing);
    }

    // ── Paso 2: Actualización post-análisis ───────────────────────────────────

    @Override
    @Transactional
    public ClothingItemResponse applyAiAudit(AuditUpdateRequest request) {
        log.info("ClothingService: Aplicando audit IA para prenda {}", request.clothingId());

        // 1. Cargar la prenda
        Clothing clothing = clothingRepository.findById(request.clothingId())
                .orElseThrow(() -> new RuntimeException(
                        "Prenda no encontrada con ID: " + request.clothingId()));

        // 2. Resolver referencias del catálogo
        ClothingCategory category = categoryRepository.findById(request.predictedCategoryId())
                .orElseThrow(() -> new RuntimeException(
                        "Categoría no encontrada: " + request.predictedCategoryId()));

        // 3. Actualizar la prenda: asignar categoría y marcar como procesada
        clothing.setCategory(category);
        clothing.setProcessed(true);
        clothingRepository.save(clothing);

        // 4. Persistir o actualizar el registro de auditoría IA
        // Usamos findOrCreate para soportar re-análisis (si el usuario pide re-procesar)
        ClothingAiAudit audit = clothingAiAuditRepository
                .findByClothingId(clothing.getId())
                .orElse(ClothingAiAudit.builder().clothing(clothing).build());

        // Resolver referencias opcionales del catálogo
        // Style, Weather y Occasion se usan en el scoring engine (Fase 5+)
        // pero las persistimos ya para tener el dato disponible
        audit.setPredictedCategory(category);
        audit.setConfidenceScore(request.confidenceScore());
        audit.setAiProvider(request.aiProvider());
        audit.setWasCorrected(false);

        // Resolver Weather
        weatherRepository.findById(request.predictedWeatherId())
                .ifPresent(audit::setPredictedWeather);

        // Resolver Occasion
        occasionRepository.findById(request.predictedOccasionId())
                .ifPresent(audit::setPredictedOccasion);

        // Resolver Style
        styleRepository.findById(request.predictedStyleId())
                .ifPresent(audit::setPredictedStyle);

        clothingAiAuditRepository.save(audit);

        log.info("ClothingService: Prenda {} procesada correctamente — categoría: {}",
                clothing.getId(), category.getName());

        return toResponse(clothing);
    }

    // ── Consultas ─────────────────────────────────────────────────────────────

    @Override
    public List<ClothingItemResponse> getWardrobeByUser(UUID userId) {
        log.info("ClothingService: Consultando guardarropa para usuario {}", userId);
        return clothingRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ClothingItemResponse getClothingById(UUID clothingId) {
        Clothing clothing = clothingRepository.findById(clothingId)
                .orElseThrow(() -> new RuntimeException(
                        "Prenda no encontrada con ID: " + clothingId));
        return toResponse(clothing);
    }

    // ── Eliminación ───────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void deleteClothing(UUID clothingId, UUID userId) {
        log.info("ClothingService: Eliminando prenda {} del usuario {}", clothingId, userId);

        if (!clothingRepository.existsByIdAndUserId(clothingId, userId)) {
            throw new RuntimeException(
                    "Prenda no encontrada o no pertenece al usuario: " + clothingId);
        }

        // Eliminar el registro de auditoría IA si existe (dependiente)
        clothingAiAuditRepository.findByClothingId(clothingId)
                .ifPresent(clothingAiAuditRepository::delete);

        clothingRepository.deleteById(clothingId);
        log.info("ClothingService: Prenda {} eliminada correctamente", clothingId);
    }

    // ── Catálogo ──────────────────────────────────────────────────────────────

    @Override
    public CatalogDTO getCatalog() {
        log.info("ClothingService: Construyendo catálogo de referencia");

        List<CatalogDTO.CategoryEntry> categories = categoryRepository
                .findByIsActiveTrueOrderByNameAsc()
                .stream()
                .map(c -> new CatalogDTO.CategoryEntry(
                        c.getId(),
                        c.getName(),
                        c.getParent() != null ? c.getParent().getId() : null))
                .collect(Collectors.toList());

        List<CatalogDTO.OccasionEntry> occasions = occasionRepository
                .findByIsActiveTrueOrderByNameAsc()
                .stream()
                .map(o -> new CatalogDTO.OccasionEntry(o.getId(), o.getName()))
                .collect(Collectors.toList());

        List<CatalogDTO.WeatherEntry> weathers = weatherRepository
                .findByIsActiveTrueOrderByNameAsc()
                .stream()
                .map(w -> new CatalogDTO.WeatherEntry(w.getId(), w.getName()))
                .collect(Collectors.toList());

        return new CatalogDTO(categories, occasions, weathers);
    }

    // ── Mapper privado ────────────────────────────────────────────────────────

    private ClothingItemResponse toResponse(Clothing clothing) {
        UUID categoryId = clothing.getCategory() != null
                ? clothing.getCategory().getId()
                : null;
        String categoryName = clothing.getCategory() != null
                ? clothing.getCategory().getName()
                : null;

        return new ClothingItemResponse(
                clothing.getId(),
                clothing.getUser().getId(),
                clothing.getImageUrl(),
                categoryId,
                categoryName,
                clothing.isProcessed(),
                clothing.getCreatedAt()
        );
    }
}