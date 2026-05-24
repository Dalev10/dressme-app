package com.dressme.dressme_back.schema.dto;
 
import java.util.UUID;
 
public record ComputeTasteVectorResponse(
    UUID userId,
    float[] tasteVector,
    int cardsUsed,
    int likesCount,
    int dislikesCount
) {}