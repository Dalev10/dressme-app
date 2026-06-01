package com.dressme.dressme_database.service.impl;
 
import com.dressme.dressme_database.model.TrendDatasetConfig;
import com.dressme.dressme_database.repository.TrendDatasetConfigRepository;
import com.dressme.dressme_database.schema.dto.TrendDatasetConfigRequest;
import com.dressme.dressme_database.schema.dto.TrendDatasetConfigResponse;
import com.dressme.dressme_database.service.TrendDatasetConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
 
@Service
@RequiredArgsConstructor
@Slf4j
public class TrendDatasetConfigServiceImpl implements TrendDatasetConfigService {
 
    private final TrendDatasetConfigRepository repository;
 
    @Override
    @Transactional
    public TrendDatasetConfigResponse save(TrendDatasetConfigRequest request) {
        log.info(
            "TrendDatasetConfig: Persistiendo vector promedio — imageCount={}, model={}",
            request.imageCount(),
            request.modelUsed()
        );
 
        TrendDatasetConfig entity = TrendDatasetConfig.builder()
            .avgVector(request.avgVector())
            .imageCount(request.imageCount())
            .modelUsed(request.modelUsed())
            .description(request.description())
            // id y computedAt los gestiona Hibernate automáticamente
            .build();
 
        TrendDatasetConfig saved = repository.saveAndFlush(entity);
 
        log.info(
            "TrendDatasetConfig: Persistido con id={} computedAt={}",
            saved.getId(),
            saved.getComputedAt()
        );
 
        return new TrendDatasetConfigResponse(
            saved.getId(),
            saved.getImageCount(),
            saved.getModelUsed(),
            saved.getDescription(),
            saved.getComputedAt()
        );
    }
}