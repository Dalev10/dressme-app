package com.dressme.dressme_database.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tbl_outfit_ai_audit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutfitAiAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "outfit_id", unique = true, nullable = false)
    private Outfit outfit;

    @Column(name = "match_score", precision = 3, scale = 2)
    private BigDecimal matchScore;

    @Column(name = "affinity_score", precision = 3, scale = 2)
    private BigDecimal affinityScore;

    /**
     * Score total compuesto [0.0–1.0].
     * Calculado por ScoreEngine: color + dresscode + taste + trend
     * con redistribución de pesos cuando algún componente no aplica.
     *
     * Es el valor de ranking definitivo del outfit.
     * Nullable: outfits anteriores al ScoreEngine no tienen este valor.
     */

    @Column(name = "total_score")
    private Double totalScore;

    // Vector matemático del outfit completo para el motor de ML
    @JdbcTypeCode(SqlTypes.VECTOR)
    @Column(name = "outfit_vector", columnDefinition = "vector(1536)")
    private float[] outfitVector;

    @Column(name = "ai_feedback_log", columnDefinition = "TEXT")
    private String aiFeedbackLog;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}