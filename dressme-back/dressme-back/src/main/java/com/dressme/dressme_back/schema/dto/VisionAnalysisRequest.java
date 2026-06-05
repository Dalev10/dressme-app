package com.dressme.dressme_back.schema.dto;

import java.util.UUID;

/**
 * Payload enviado a POST /ai/wardrobe/analyze en dressme-ai.
 *
 * Contiene solo la información necesaria para que Gemini Vision analice
 * la prenda: su ID interno y la URL donde obtener la imagen.
 *
 * dressme-back → dressme-ai (contrato HTTP interservicio)
 */
public record VisionAnalysisRequest(
        UUID clothingId,
        String imageUrl
) {}
