package com.dressme.dressme_database.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "tbl_clothes_occasions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClothingOccasion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clothes_id", nullable = false)
    private Clothing clothing;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "occasion_id", nullable = false)
    private Occasion occasion;
}