package com.dressme.dressme_back.schema.dto;

import java.util.UUID;

public record OutfitDressCodeResponse(
    UUID outfitId,
    UUID dressCodeId
) {}