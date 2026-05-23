package com.dressme.dressme_database.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "tbl_color_compatibility")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ColorCompatibility {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "color_a_id", nullable = false)
    private Color colorA;

    @ManyToOne
    @JoinColumn(name = "color_b_id", nullable = false)
    private Color colorB;

    @Column(name = "compatibility_score", nullable = false)
    private Double compatibilityScore;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private java.time.LocalDateTime createdAt = java.time.LocalDateTime.now();
}
