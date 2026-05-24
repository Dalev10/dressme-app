package com.dressme.dressme_back.schema.dto; 

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
    private String sourceType;  

    @JsonCreator // Obligamos a Jackson a usar este constructor
    public UserUpdateRequest(
        @JsonProperty("displayName") String displayName,
        @JsonProperty("profilePicture") String profilePicture,
        @JsonProperty("isCalibrated") Boolean isCalibrated,
        @JsonProperty("tasteVector") float[] tasteVector,
        @JsonProperty("sourceType") String sourceType
    ) {
        this.displayName = displayName;
        this.profilePicture = profilePicture;
        this.isCalibrated = isCalibrated;
        this.tasteVector = tasteVector;
        this.sourceType = sourceType;
    }

    public UserUpdateRequest() {} // Constructor vacío necesario
}