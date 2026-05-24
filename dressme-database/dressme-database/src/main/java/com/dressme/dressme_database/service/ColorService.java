package com.dressme.dressme_database.service;

import com.dressme.dressme_database.model.Color;
import com.dressme.dressme_database.schema.dto.ColorResponseDTO;

import java.util.List;
import java.util.UUID;

public interface ColorService {
    ColorResponseDTO getComplementaryColor(UUID baseColorId);
    List<ColorResponseDTO> getAnalogousColors(UUID baseColorID);
}
