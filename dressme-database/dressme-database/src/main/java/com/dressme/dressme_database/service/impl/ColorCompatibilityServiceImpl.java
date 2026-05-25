package com.dressme.dressme_database.service.impl;

import com.dressme.dressme_database.repository.ColorCompatibilityRepository;
import com.dressme.dressme_database.service.ColorCompatibilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ColorCompatibilityServiceImpl implements ColorCompatibilityService {
    private final ColorCompatibilityRepository colorCompatibilityRepository;

    @Override
    public Optional<Double> getCompatibilityScore(UUID colorId1, UUID colorId2) {
        return colorCompatibilityRepository.findScoreByColorIds(colorId1, colorId2);
    }
}
