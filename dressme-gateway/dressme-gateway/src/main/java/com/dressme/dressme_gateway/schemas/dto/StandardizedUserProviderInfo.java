package com.dressme.dressme_gateway.schemas.dto;

/**
 * Contrato estandarizado que el Gateway construirá y enviará internamente 
 * hacia dressme-back, sin importar de qué proveedor provengan los datos.
 */
public record StandardizedUserProviderInfo(
    String provider, // "GOOGLE" o "PINTEREST"
    String providerId,
    String email,
    String displayName,
    String profilePictureUrl
) {}