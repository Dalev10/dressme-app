package com.dressme.dressme_gateway.schema.dto;
 
import java.util.UUID;
 
public record StyleCardDTO(
    UUID id,
    String name,
    String semanticDescription,
    String imageUrl,
    String tags,
    Integer displayOrder
) {}