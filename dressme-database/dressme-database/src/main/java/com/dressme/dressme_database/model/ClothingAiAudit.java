package com.dressme.dressme_database.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tbl_clothing_ai_audit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClothingAiAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clothes_id", unique = true, nullable = false)
    private Clothing clothing;

    @Column(name = "confidence_score", precision = 3, scale = 2) 
    private BigDecimal confidenceScore;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "predicted_category_id")
    private ClothingCategory predictedCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "predicted_style_id")
    private Style predictedStyle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "predicted_color_id")
    private Color predictedColor;

    /** Valores HSL exactos detectados por Gemini Vision (independientes del color canónico) */
    @Column(name = "detected_hue")
    private Integer detectedHue;

    @Column(name = "detected_saturation")
    private Integer detectedSaturation;

    @Column(name = "detected_lightness")
    private Integer detectedLightness;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "predicted_weather_id")
    private Weather predictedWeather;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "predicted_occasion_id")
    private Occasion predictedOccasion;

    @Builder.Default
    @Column(name = "was_corrected")
    private boolean wasCorrected = false;

    @Column(name = "ai_provider")
    private String aiProvider; // 'google_vision', 'clarifai'

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}