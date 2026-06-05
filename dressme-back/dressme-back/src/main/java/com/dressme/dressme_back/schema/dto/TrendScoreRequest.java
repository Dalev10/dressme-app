package com.dressme.dressme_back.schema.dto;
 
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
 
/**
 * Request que dressme-back envía al servicio AI para calcular el trend score.
 *
 * outfit_embeddings → Lista de embeddings de las prendas del outfit (1536 dims c/u).
 *                     El AI los promedia internamente para obtener el outfit_vector.
 */
public record TrendScoreRequest(
    @NotEmpty(message = "Se requiere al menos un embedding de prenda")
    List<List<Float>> outfitEmbeddings
) {}