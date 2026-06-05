package com.dressme.dressme_database.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "tbl_dress_code_compatibility")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(DressCodeCompatibilityId.class)
public class DressCodeCompatibility {

    @Id
    @Column(name = "dress_code_id")
    private UUID dressCodeId;

    @Id
    @Column(name = "compatible_with_id")
    private UUID compatibleWithId;
}
