package com.dressme.dressme_database.service;

import com.dressme.dressme_database.schema.dto.DressCodeDTO;

import java.util.List;
import java.util.UUID;

public interface DressCodeService {
    List<DressCodeDTO> getActiveDressCodes();
    List<UUID> getCompatibleDressCodeIds(UUID dressCodeId);
}