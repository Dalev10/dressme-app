package com.dressme.dressme_database.service.impl;

import com.dressme.dressme_database.model.DressCode;
import com.dressme.dressme_database.model.DressCodeCompatibility;
import com.dressme.dressme_database.repository.DressCodeCompatibilityRepository;
import com.dressme.dressme_database.repository.DressCodeRepository;
import com.dressme.dressme_database.schema.dto.DressCodeDTO;
import com.dressme.dressme_database.service.DressCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DressCodeServiceImpl implements DressCodeService {

    private final DressCodeRepository dressCodeRepository;
    private final DressCodeCompatibilityRepository compatibilityRepository;

    @Override
    @Transactional(readOnly = true)
    public List<DressCodeDTO> getActiveDressCodes() {
        log.info("DressCodeService: Consultando dress codes activos");
        return dressCodeRepository.findByIsActiveTrueOrderByNameAsc().stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> getCompatibleDressCodeIds(UUID dressCodeId) {
        log.info("DressCodeService: Consultando compatibles para dress code {}", dressCodeId);
        return compatibilityRepository.findByDressCodeId(dressCodeId).stream()
                .map(DressCodeCompatibility::getCompatibleWithId)
                .toList();
    }

    private DressCodeDTO toDto(DressCode dressCode) {
        return new DressCodeDTO(
                dressCode.getId(),
                dressCode.getName(),
                dressCode.getDescription(),
                dressCode.getEmbeddingVector(),
                dressCode.isActive()
        );
    }
}