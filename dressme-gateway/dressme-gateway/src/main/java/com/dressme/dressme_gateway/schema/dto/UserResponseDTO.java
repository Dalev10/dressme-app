package com.dressme.dressme_gateway.schema.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.UUID;

@Data
public class UserResponseDTO {
    private UUID id;
    private String email;
    private String name;
    private String pictureUrl;
    @JsonProperty("isCalibrated")
    private boolean isCalibrated;
}