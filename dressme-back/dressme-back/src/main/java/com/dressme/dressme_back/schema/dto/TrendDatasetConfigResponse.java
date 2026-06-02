package com.dressme.dressme_back.schema.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record TrendDatasetConfigResponse(
        UUID id,
        List<Float> avgVector,
        int imageCount,
        String modelUsed,
        String description,
        LocalDateTime computedAt
) {}