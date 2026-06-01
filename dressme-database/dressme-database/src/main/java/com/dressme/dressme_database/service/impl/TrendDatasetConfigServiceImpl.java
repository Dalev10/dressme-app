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
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
 
import java.util.List;
 
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

        List<Float> vectorAsList = new java.util.ArrayList<>();
        for (float f : saved.getAvgVector()) {
            vectorAsList.add(f);
        }

        return new TrendDatasetConfigResponse(
            saved.getId(),
            vectorAsList,
            saved.getImageCount(),
            saved.getModelUsed(),
            saved.getDescription(),
            saved.getComputedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public TrendDatasetConfigResponse getLatest() {
        log.info("TrendDatasetConfig: Buscando el vector de dataset más reciente");

        TrendDatasetConfig latest = repository.findLatest()
            .orElseThrow(() -> {
                log.warn("TrendDatasetConfig: No hay vector de dataset disponible");
                return new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No hay vector de dataset disponible. Ejecuta el script de cómputo primero."
                );
            });

        log.info(
            "TrendDatasetConfig: Vector encontrado — id={} imageCount={} computedAt={}",
            latest.getId(),
            latest.getImageCount(),
            latest.getComputedAt()
        );

        List<Float> vectorAsList = new java.util.ArrayList<>();
        for (float f : latest.getAvgVector()) {
            vectorAsList.add(f);
        }

        return new TrendDatasetConfigResponse(
            latest.getId(),
            vectorAsList,
            latest.getImageCount(),
            latest.getModelUsed(),
            latest.getDescription(),
            latest.getComputedAt()
        );
    }
}