package com.dressme.dressme_database.schema.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Payload que envía dressme-back cuando el orquestador persiste un outfit
 * generado por dressme-ai + ScoreEngine.
 *
 * Contiene los IDs de las prendas seleccionadas, los scores calculados
 * y el vector del outfit para similitud futura con el taste_vector.
 */
public record OutfitCreateRequest(

    @NotNull(message = "El userId es obligatorio")
    UUID userId,

    @NotEmpty(message = "El outfit debe tener al menos una prenda")
    List<UUID> clothingIds,

    /** DressCode inferido por el motor de recomendación (nullable) */
    UUID dressCodeId,

    /** Ocasión para la que se generó el outfit (nullable) */
    UUID occasionId,

    /** Clima para el que se generó el outfit (nullable) */
    UUID weatherId,

    /** Nombre sugerido por Gemini (nullable) */
    String name,

    /**
     * Score compuesto del ScoreEngine [0.00–1.00].
     * color (40%) + dresscode (20%) + taste (20%) + trend (20%).
     */
    @NotNull
    @DecimalMin("0.00") @DecimalMax("1.00")
    BigDecimal matchScore,

    /**
     * Similitud coseno entre outfit_vector y taste_vector [0.00–1.00].
     */
    @NotNull
    @DecimalMin("0.00") @DecimalMax("1.00")
    BigDecimal affinityScore,

    /**
     * Vector semántico del outfit (1536 dims).
     * Promedio ponderado de los embedding_vector de las prendas.
     * Nullable hasta que todas las prendas estén vectorizadas.
     */
    float[] outfitVector

) {}