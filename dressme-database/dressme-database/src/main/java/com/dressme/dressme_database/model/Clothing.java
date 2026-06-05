package com.dressme.dressme_database.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Representa una prenda del guardarropa de un usuario.
 *
 * DISEÑO DELIBERADO: category_id es nullable.
 * El flujo de wardrobe es asíncrono en dos pasos:
 *   1. Se crea la prenda con imagen y is_processed=false (category puede ser null)
 *   2. Vision + Gemini analizan la imagen y actualizan category + is_processed=true
 *
 * La categoría "Uncategorized" del seed (UUID fijo: 55555555-0000-0000-0000-000000000001)
 * se usa como valor semántico cuando la IA aún no procesó la prenda,
 * pero category_id = null es igualmente válido durante el período de análisis.
 * Esto evita FK constraints que bloqueen el INSERT inicial.
 *
 * MIGRACIÓN embedding_vector TEXT → vector(1536):
 *   La columna era TEXT (JSON serializado) — inconsistente con outfit_vector
 *   y taste_vector que usan vector(1536) nativo de pgvector.
 *   Migrada a vector(1536) para habilitar similitud coseno directa con <=>
 *   en PostgreSQL, igual que OutfitAiAudit y UserTasteProfile.
 *   Script de migración: V3__migrate_clothing_embedding_to_vector.sql
 */
@Entity
@Table(name = "tbl_clothes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Clothing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * nullable = true: la categoría se asigna tras el análisis de Vision.
     * Antes era nullable = false, lo que impedía el INSERT inicial de la prenda
     * antes de que la IA terminara el análisis.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = true)
    private ClothingCategory category;

    @Column(name = "image_url", nullable = false, columnDefinition = "TEXT")
    private String imageUrl;

    @Builder.Default
    @Column(name = "is_processed")
    private boolean isProcessed = false;

    /**
     * Vector semántico de 1536 dimensiones que representa la prenda.
     * Generado por dressme-ai combinando: categoría + estilo + color + clima + ocasión.
     *
     * Nullable: null hasta que el flujo de recomendación lo genere.
     * Se re-genera cuando is_embedding_stale = true (el usuario corrigió la prenda).
     *
     * Mismo tipo que taste_vector (UserTasteProfile) y outfit_vector (OutfitAiAudit),
     * lo que permite usar similitud coseno nativa de pgvector (<=>)
     * para comparar prendas contra el gusto del usuario directamente en SQL.
     */
    @JdbcTypeCode(SqlTypes.VECTOR)
    @Column(name = "embedding_vector", columnDefinition = "vector(1536)")
    private float[] embeddingVector;

    @Builder.Default
    @Column(name = "is_embedding_stale")
    private boolean isEmbeddingStale = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}