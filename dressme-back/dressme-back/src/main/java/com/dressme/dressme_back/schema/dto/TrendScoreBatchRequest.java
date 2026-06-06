package com.dressme.dressme_back.schema.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record TrendScoreBatchRequest(
    @NotEmpty
    List<List<List<Float>>> outfits
) {}
