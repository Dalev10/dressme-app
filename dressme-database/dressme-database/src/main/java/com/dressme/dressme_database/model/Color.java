package com.dressme.dressme_database.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "tbl_colors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Color {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    private Integer hue;

    @Column(nullable = false)
    private Integer saturation;

    @Column(nullable = false)
    private Integer lightness;

    @Builder.Default
    @Column(name = "is_neutral")
    private boolean isNeutral = false;
}
