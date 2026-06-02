package com.dressme.dressme_back.service;

import java.util.UUID;

public interface DressCodeScoreService {
    double score(UUID outfitId, UUID userId);
}