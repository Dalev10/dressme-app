package com.dressme.dressme_database.service;

import java.util.Optional;
import java.util.UUID;

public interface ColorCompatibilityService {
    Optional<Double> getCompatibilityScore(UUID colorId1, UUID colorId2);
}
