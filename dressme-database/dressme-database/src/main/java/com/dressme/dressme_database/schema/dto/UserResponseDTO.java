package com.dressme.dressme_database.schema.dto;

import com.fasterxml.jackson.annotation.JsonProperty; // Importante
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserResponseDTO {
    private UUID id;
    private String email;
    private String displayName;    
    private String profilePicture;

    @JsonProperty("isCalibrated") // Forzamos el nombre exacto en el JSON
    private boolean isCalibrated; 
}