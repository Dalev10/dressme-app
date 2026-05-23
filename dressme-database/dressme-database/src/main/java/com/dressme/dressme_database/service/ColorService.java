package com.dressme.dressme_database.service;

import com.dressme.dressme_database.model.Color;

import java.util.List;

public interface ColorService {
    Color getComplementaryColor(Color baseColor);
    List<Color> getAnalogousColors(Color baseColor);
}
