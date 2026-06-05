package com.dressme.dressme_database.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Set;
import java.util.UUID;

/**
 * Representa una tarjeta visual de onboarding del estilo "¿Te gusta este look?".
 *
 * Cada tarjeta tiene una descripción semántica que OpenAI ya procesó y cuyo
 * embedding vive en `embedding_vector`. Durante la Fase 2 (dressme-ai) este
 * vector se usa para calcular el taste_vector del usuario.
 */
@Entity
@Table(name = "tbl_style_cards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StyleCard {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Nombre corto mostrado en la UI.
     * Ej: "Minimalist", "Streetwear", "Bohemian".
     */
    @Column(unique = true, nullable = false, length = 100)
    private String name;

    /**
     * Descripción semántica enviada a OpenAI text-embedding-3-small para
     * generar el embedding_vector. Debe ser rica en keywords de moda.
     */
    @Column(name = "semantic_description", nullable = false, columnDefinition = "TEXT")
    private String semanticDescription;

    /**
     * URL de la imagen representativa. En el MVP puede apuntar a recursos
     * estáticos del frontend o a un CDN.
     */
    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    /**
     * Tags de apoyo para filtros futuros. Almacenado como JSON string.
     * Ej: '["neutral", "clean", "monochrome"]'
     */
    @Column(columnDefinition = "TEXT")
    private String tags;

    /**
     * Vector pre-computado por OpenAI text-embedding-3-small (1536 dims).
     * Se persiste una sola vez al hacer seed de los datos.
     * NULL = embedding aún no generado (Fase 2 lo completa).
     */
    @JdbcTypeCode(SqlTypes.VECTOR)
    @Column(name = "embedding_vector", columnDefinition = "vector(1536)")
    private float[] embeddingVector;

    /**
     * Orden de presentación en la pantalla de onboarding.
     * Las tarjetas se muestran de menor a mayor display_order.
     */
    @Builder.Default
    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @Builder.Default
    @Column(name = "is_active")
    private boolean isActive = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "tbl_style_card_mappings", // El nombre exacto de la tabla puente
        joinColumns = @JoinColumn(name = "style_card_id"), // La FK hacia StyleCard
        inverseJoinColumns = @JoinColumn(name = "style_id") // La FK hacia Style
    )
    private Set<Style> microStyles;
}
