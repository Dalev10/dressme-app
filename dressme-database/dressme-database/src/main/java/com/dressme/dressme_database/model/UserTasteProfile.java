package com.dressme.dressme_database.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tbl_user_taste_profile")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserTasteProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Relación 1:1 - Cada usuario tiene un único perfil de ADN estético
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    // El "Mapa de Calor" de gustos en 1536 dimensiones
    @JdbcTypeCode(SqlTypes.VECTOR)
    @Column(name = "taste_vector", columnDefinition = "vector(1536)", nullable = false)
    private float[] tasteVector;

    @Column(name = "source_type", nullable = false)
    private String sourceType; // 'pinterest', 'google_onboarding', 'manual_game'

    @Builder.Default
    @Column(name = "is_calibrated")
    private boolean isCalibrated = false;

    @UpdateTimestamp
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
}
