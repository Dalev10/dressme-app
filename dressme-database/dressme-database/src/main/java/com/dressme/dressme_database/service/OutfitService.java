package com.dressme.dressme_database.service;

import com.dressme.dressme_database.schema.dto.OutfitDressCodeResponse;

import java.util.UUID;

public interface OutfitService {
    OutfitDressCodeResponse getDressCode(UUID outfitId);
}