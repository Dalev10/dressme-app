package com.dressme.dressme_gateway.schema.dto; // Ajusta el paquete según el servicio

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserUpdateRequest {
    private String displayName;
    private String profilePicture;
    private Boolean isCalibrated;
    private float[] tasteVector;

    @JsonCreator // Obligamos a Jackson a usar este constructor
    public UserUpdateRequest(
        @JsonProperty("displayName") String displayName,
        @JsonProperty("profilePicture") String profilePicture,
        @JsonProperty("isCalibrated") Boolean isCalibrated,
        @JsonProperty("tasteVector") float[] tasteVector
    ) {
        this.displayName = displayName;
        this.profilePicture = profilePicture;
        this.isCalibrated = isCalibrated;
        this.tasteVector = tasteVector;
    }

    public UserUpdateRequest() {} // Constructor vacío necesario
}