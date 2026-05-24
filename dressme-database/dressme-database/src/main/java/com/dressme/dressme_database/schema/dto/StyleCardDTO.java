package com.dressme.dressme_database.schema.dto;
 
import java.util.UUID;
 
public record StyleCardDTO(
    UUID id,
    String name,
    String semanticDescription,
    String imageUrl,
    String tags,
    Integer displayOrder
) {}