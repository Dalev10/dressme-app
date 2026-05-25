package com.dressme.dressme_database.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Registra la reacción de un usuario a cada tarjeta de onboarding.
 *
 * Sirve para dos propósitos:
 *   1. Alimentar el cálculo del taste_vector inicial (Fase 2).
 *   2. Guardar historial para re-entrenamientos futuros del perfil estético.
 */
@Entity
@Table(
    name = "tbl_onboarding_selections",
    uniqueConstraints = {
        // Un usuario solo puede reaccionar una vez a cada tarjeta
        @UniqueConstraint(
            name = "uq_user_style_card",
            columnNames = {"user_id", "style_card_id"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class  UserOnboardingSelection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "style_card_id", nullable = false)
    private StyleCard styleCard;

    /**
     * Reacción del usuario a la tarjeta.
     * LIKE    → peso +1.0 en el cálculo del vector
     * DISLIKE → peso -0.3 (señal negativa suave)
     * SKIP    → peso  0.0 (no aporta información)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Reaction reaction;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum Reaction {
        LIKE, DISLIKE, SKIP
    }
}
