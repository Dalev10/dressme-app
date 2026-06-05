package com.dressme.dressme_back.service;

import com.dressme.dressme_back.schema.dto.TrendScoreResponse;

import java.util.List;

public interface TrendScoreService {
    TrendScoreResponse computeTrendScore(List<List<Float>> outfitEmbeddings);
}