package com.dressme.dressme_back.schema.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.UUID;

public record ComputeTasteVectorResponse(
    @JsonProperty("user_id") UUID userId,
    @JsonProperty("taste_vector") List<Float> tasteVector,
    @JsonProperty("cards_used") Integer cardsUsed,
    @JsonProperty("likes_count") Integer likesCount,
    @JsonProperty("dislikes_count") Integer dislikesCount
) {}