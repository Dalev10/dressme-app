package com.dressme.dressme_database.schema.dto;

import java.util.UUID;

public record OutfitDressCodeResponse(
    UUID outfitId,
    UUID dressCodeId
) {}