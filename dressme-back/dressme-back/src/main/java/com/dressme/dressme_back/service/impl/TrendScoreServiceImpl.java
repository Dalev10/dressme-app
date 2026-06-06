package com.dressme.dressme_back.service.impl;

import com.dressme.dressme_back.client.AiTrendScoreClient;
import com.dressme.dressme_back.schema.dto.TrendScoreBatchRequest;
import com.dressme.dressme_back.schema.dto.TrendScoreBatchResponse;
import com.dressme.dressme_back.schema.dto.TrendScoreRequest;
import com.dressme.dressme_back.schema.dto.TrendScoreResponse;
import com.dressme.dressme_back.service.TrendScoreService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrendScoreServiceImpl implements TrendScoreService {

    private final AiTrendScoreClient aiTrendScoreClient;

    @Override
    public TrendScoreResponse computeTrendScore(List<List<Float>> outfitEmbeddings) {
        if (outfitEmbeddings == null || outfitEmbeddings.isEmpty()) {
            log.warn("TrendScoreService: No se recibieron embeddings. Devolviendo applies=false.");
            return new TrendScoreResponse(0.0, false, 0);
        }

        log.info("TrendScoreService: Solicitando trend score para outfit con {} prendas",
            outfitEmbeddings.size());

        try {
            TrendScoreRequest request = new TrendScoreRequest(outfitEmbeddings);
            TrendScoreResponse response = aiTrendScoreClient.scoreTrend(request);

            log.info(
                "TrendScoreService: trend_score={} applies={} dataset_images={}",
                response.trendScore(),
                response.applies(),
                response.datasetImages()
            );

            return response;

        } catch (FeignException e) {
            log.error(
                "TrendScoreService: El servicio AI falló al calcular trend score. "
                + "status={}, body={}. Devolviendo applies=false para no bloquear el scoring.",
                e.status(),
                e.contentUTF8()
            );
            return new TrendScoreResponse(0.0, false, 0);
        }
    }

    @Override
    public TrendScoreBatchResponse computeBatch(List<List<List<Float>>> outfitsEmbeddings) {
        if (outfitsEmbeddings == null || outfitsEmbeddings.isEmpty()) {
            log.warn("TrendScoreService: computeBatch llamado sin outfits. Devolviendo lista vacía.");
            return new TrendScoreBatchResponse(List.of());
        }

        log.info("TrendScoreService: Solicitando trend score batch para {} outfits", outfitsEmbeddings.size());

        try {
            TrendScoreBatchResponse response = aiTrendScoreClient.scoreTrendBatch(
                    new TrendScoreBatchRequest(outfitsEmbeddings));

            log.info("TrendScoreService: computeBatch completado — {} scores recibidos", response.scores().size());
            return response;

        } catch (FeignException e) {
            log.error(
                "TrendScoreService: El servicio AI falló en score batch. "
                + "status={}, body={}. Devolviendo applies=false para todos los outfits.",
                e.status(),
                e.contentUTF8()
            );
            List<TrendScoreResponse> fallback = outfitsEmbeddings.stream()
                    .map(ignored -> new TrendScoreResponse(0.0, false, 0))
                    .collect(Collectors.toList());
            return new TrendScoreBatchResponse(fallback);
        }
    }
}