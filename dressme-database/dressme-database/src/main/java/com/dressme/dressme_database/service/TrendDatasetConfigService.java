package com.dressme.dressme_database.service;
 
import com.dressme.dressme_database.schema.dto.TrendDatasetConfigRequest;
import com.dressme.dressme_database.schema.dto.TrendDatasetConfigResponse;
 
public interface TrendDatasetConfigService {
 
    /**
     * Persiste un nuevo vector promedio del dataset de moda actual.
     * Cada llamada inserta una nueva fila; la más reciente es la activa.
     */
    TrendDatasetConfigResponse save(TrendDatasetConfigRequest request);
}