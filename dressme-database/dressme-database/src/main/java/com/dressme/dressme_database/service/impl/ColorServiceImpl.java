package com.dressme.dressme_database.service.impl;

import com.dressme.dressme_database.model.Color;
import com.dressme.dressme_database.repository.ColorRepository;
import com.dressme.dressme_database.service.ColorService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ColorServiceImpl implements ColorService {
    private final ColorRepository colorRepository;

    public ColorServiceImpl(ColorRepository colorRepository) {
        this.colorRepository = colorRepository;
    }

    @Override
    public Color getComplementaryColor(Color baseColor){
        if (baseColor.isNeutral()) return null;

        int targetHue = (baseColor.getHue() + 180) % 360;
        List<Color> nonNeutralColors = colorRepository.findByIsNeutralFalse();

        /* Margen de toleracia para encontrar un color similar en la DB
        Ej: se busca un amarillo con Hue 65, pero en la DB tenemos
        amarillo con Hue 60
         */
        final int TOLERANCE = 15;

        return nonNeutralColors.stream()
            .filter(color ->{
            int diff = Math.abs(color.getHue() - targetHue);
            int distance = Math.min(diff, 360 - diff);
            return distance <= TOLERANCE;
        })
            .findFirst()
            .orElse(null);
    }

    @Override
    public List<Color> getAnalogousColors(Color baseColor){
        if (baseColor.isNeutral()) return new ArrayList<>();
        int upperAnalogousHue = (baseColor.getHue() + 30) % 360;
        int lowerAnalogousHue = (baseColor.getHue() - 30 + 360) % 360;

        final int TOLERANCE = 15;

        List<Color> nonNeutralColors = colorRepository.findByIsNeutralFalse();

        return nonNeutralColors.stream()
            .filter(color -> {
                int diffUpper = Math.abs(color.getHue() - upperAnalogousHue);
                int distanceToUpper = Math.min(diffUpper, 360 - diffUpper);
                int diffLower = Math.abs(color.getHue() - lowerAnalogousHue);
                int distanceToLower = Math.min(diffLower, 360 - diffLower);

                return distanceToUpper <= TOLERANCE || distanceToLower <= TOLERANCE;
            })
            .toList();
    }
}
