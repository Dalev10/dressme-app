package com.dressme.dressme_back.service;

import java.util.UUID;

/**
 * Calcula el score de compatibilidad entre un outfit y el dress code del usuario.
 *
 * NOTA DE ARQUITECTURA:
 * Este servicio se usa exclusivamente en el flujo de scoring standalone
 * (InternalScoreEngineController → ScoreEngineOrchestratorService),
 * donde el dress code se infiere del taste_vector del usuario comparándolo
 * por cosine similarity contra todos los dress codes activos.
 *
 * En el flujo de outfit generation orquestado (OutfitOrchestratorServiceImpl),
 * el dresscode_score llega precalculado desde dressme-ai, que calcula
 * la similitud coseno entre el outfit_vector del candidato y el
 * embeddingVector del dress code explícitamente seleccionado por el usuario.
 * En ese flujo este servicio NO se invoca.
 */

public interface DressCodeScoreService {
    double score(UUID outfitId, UUID userId);
}