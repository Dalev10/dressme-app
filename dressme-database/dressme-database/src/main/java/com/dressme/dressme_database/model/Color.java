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

    @Column(name = "hex_code", unique = true, nullable = false, length = 7)
    private String hexCode; // Ejemplo: #FF5733

    @Builder.Default
    @Column(name = "is_neutral")
    private boolean isNeutral = false;
}