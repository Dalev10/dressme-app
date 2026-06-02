package com.dressme.dressme_database.service.impl;

import com.dressme.dressme_database.model.Outfit;
import com.dressme.dressme_database.repository.OutfitRepository;
import com.dressme.dressme_database.schema.dto.OutfitDressCodeResponse;
import com.dressme.dressme_database.service.OutfitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutfitServiceImpl implements OutfitService {

    private final OutfitRepository outfitRepository;

    @Override
    @Transactional(readOnly = true)
    public OutfitDressCodeResponse getDressCode(UUID outfitId) {
        Outfit outfit = outfitRepository.findById(outfitId)
                .orElseThrow(() -> new RuntimeException("Outfit no encontrado con ID: " + outfitId));

        UUID dressCodeId = outfit.getDressCode() != null ? outfit.getDressCode().getId() : null;

        log.info("OutfitService: Outfit {} tiene dressCode {}", outfitId, dressCodeId);

        return new OutfitDressCodeResponse(outfitId, dressCodeId);
    }
}