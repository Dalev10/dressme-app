package com.dressme.dressme_database.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tbl_trend_dataset_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrendDatasetConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JdbcTypeCode(SqlTypes.VECTOR)
    @Column(name = "avg_vector", columnDefinition = "vector(1536)", nullable = false)
    private float[] avgVector;

    @Column(name = "image_count", nullable = false)
    private int imageCount;

    @Column(name = "model_used", length = 100, nullable = false)
    private String modelUsed;

    @Column(columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(name = "computed_at", updatable = false)
    private LocalDateTime computedAt;
}