package com.dressme.dressme_back.service.impl;

import com.dressme.dressme_back.schema.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.UUID;

/**
 * Análisis de visión ejecutado de forma asíncrona.
 *
 * Flujo:
 *   1. POST /ai/wardrobe/analyze       → categoría, estilo, color, clima, ocasión (UUIDs)
 *   2. PATCH /internal/wardrobe/audit  → persiste el audit en dressme-database
 *
 * Nota:
 *   El embedding vectorial ya no se genera aquí.
 *   Ahora lo resuelve el orquestador en Step 0, antes de generar outfits.
 *
 * Existe como bean separado para que @Async sea proxeado correctamente por
 * Spring AOP (self-invocation no es interceptada por el proxy).
 *
 * Política de reintentos:
 *   dressme-ai ya reintenta internamente 3 veces con backoff antes de responder.
 *   Este retry en Java es una segunda línea de defensa para el caso en que
 *   dressme-ai agote sus propios intentos y devuelva 503, o para errores de red
 *   transitorios entre dressme-back y dressme-ai (cortes breves, cold start).
 *
 *   Solo se reintenta ante 503 (proveedor AI saturado) y 429 (rate-limit).
 *   Errores 4xx distintos (400, 401, 404) son deterministas y no se reintentan.
 *
 * Timeout:
 *   readTimeout = 95s para cubrir el peor caso de dressme-ai:
 *     descarga de imagen (~2s) + 3 intentos Gemini con backoff (~85s) + margen.
 */
@Service
@Slf4j
public class AsyncVisionService {

    // ── Política de reintentos ────────────────────────────────────────────────
    // MAX_RETRIES:     intentos adicionales tras el primero (total = 1 + 2 = 3).
    // RETRY_DELAYS_MS: espera antes de cada reintento en milisegundos.
    //                  Valores conservadores: dressme-ai ya esperó su propio backoff,
    //                  aquí solo damos tiempo para que el estado del proveedor mejore.
    private static final int    MAX_RETRIES      = 2;
    private static final long[] RETRY_DELAYS_MS  = {3_000, 8_000};

    private final RestClient databaseClient;
    private final RestClient aiVisionClient;

    public AsyncVisionService(
            RestClient.Builder restClientBuilder,
            @org.springframework.beans.factory.annotation.Value("${app.services.database-url}") String databaseUrl,
            @org.springframework.beans.factory.annotation.Value("${app.services.ai-url}")       String aiUrl
    ) {
        this.databaseClient = restClientBuilder.clone().baseUrl(databaseUrl).build();

        // readTimeout ajustado a 95s para cubrir el peor caso de dressme-ai:
        // la cadena de reintentos internos de Python puede tardar hasta ~87s
        // (descarga imagen + 3 intentos Gemini con backoff de 2s y 5s).
        // Con 35s el cliente Java cortaba la conexión antes de que Python
        // terminara su último reintento, provocando el error en el frontend.
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        factory.setReadTimeout(Duration.ofSeconds(95));
        this.aiVisionClient = RestClient.builder().requestFactory(factory).baseUrl(aiUrl).build();
    }

    @Async
    public void triggerVisionAnalysis(UUID clothingId, String imageUrl) {
        log.info("Back-Wardrobe[async]: Iniciando análisis completo para prenda {}", clothingId);
        try {
            // ── Paso 1: análisis de visión con retry ──────────────────────────
            VisionAnalysisResponse aiResponse = callAiWithRetry(clothingId, imageUrl);

            if (aiResponse == null) {
                log.warn("Back-Wardrobe[async]: Respuesta nula de visión para prenda {}", clothingId);
                return;
            }

            // ── Paso 2: guardar audit ─────────────────────────────────────────
            databaseClient.patch()
                    .uri("/internal/wardrobe/audit")
                    .body(new AuditUpdateRequest(
                            aiResponse.clothingId(),
                            aiResponse.predictedCategoryId(),
                            aiResponse.predictedStyleId(),
                            aiResponse.predictedColorId(),
                            aiResponse.detectedHue(),
                            aiResponse.detectedSaturation(),
                            aiResponse.detectedLightness(),
                            aiResponse.predictedWeatherIds(),
                            aiResponse.predictedOccasionIds(),
                            aiResponse.confidenceScore(),
                            aiResponse.aiProvider()))
                    .retrieve()
                    .toBodilessEntity();

            log.info("Back-Wardrobe[async]: Audit guardado para prenda {}", clothingId);
            log.info("Back-Wardrobe[async]: Pipeline de visión finalizado para prenda {}", clothingId);

        } catch (Exception e) {
            log.error("Back-Wardrobe[async]: Error en pipeline de visión para prenda {}: {}",
                    clothingId, e.getMessage());
        }
    }

    /**
     * Llama a POST /ai/wardrobe/analyze con hasta MAX_RETRIES reintentos.
     *
     * Solo reintenta ante respuestas 503 (Service Unavailable) y 429 (Too Many Requests).
     * Cualquier otro error HTTP (4xx, 500) o de red se lanza inmediatamente
     * para que el catch de triggerVisionAnalysis lo registre sin desperdiciar tiempo.
     *
     * @throws RetryableAiException si dressme-ai responde 503 o 429 en todos los intentos.
     * @throws RuntimeException     para cualquier otro error no reintentable.
     */
    private VisionAnalysisResponse callAiWithRetry(UUID clothingId, String imageUrl) {
        RetryableAiException lastRetryableError = null;

        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {

            if (attempt > 0) {
                long delay = RETRY_DELAYS_MS[attempt - 1];
                log.warn(
                    "Back-Wardrobe[async]: Reintento {}/{} para prenda {} — esperando {}ms (error previo: {})",
                    attempt, MAX_RETRIES, clothingId, delay,
                    lastRetryableError != null ? lastRetryableError.getMessage() : "desconocido"
                );
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrumpido para prenda " + clothingId, ie);
                }
            }

            try {
                log.info(
                    "Back-Wardrobe[async]: Llamando a dressme-ai — prenda={}, intento={}/{}",
                    clothingId, attempt + 1, MAX_RETRIES + 1
                );

                return aiVisionClient.post()
                        .uri("/ai/wardrobe/analyze")
                        .body(new VisionAnalysisRequest(clothingId, imageUrl))
                        .retrieve()
                        .onStatus(
                            status -> status.value() == 503 || status.value() == 429,
                            (req, res) -> {
                                throw new RetryableAiException(
                                    "dressme-ai respondió " + res.getStatusCode().value()
                                    + " para prenda " + clothingId
                                );
                            }
                        )
                        .onStatus(
                            HttpStatusCode::isError,
                            (req, res) -> {
                                // 4xx distintos de 429, o 5xx distintos de 503:
                                // errores deterministas, no tiene sentido reintentar.
                                throw new RuntimeException(
                                    "dressme-ai (vision) falló con " + res.getStatusCode().value()
                                    + " para prenda " + clothingId
                                );
                            }
                        )
                        .body(VisionAnalysisResponse.class);

            } catch (RetryableAiException e) {
                lastRetryableError = e;
                log.warn(
                    "Back-Wardrobe[async]: dressme-ai no disponible — prenda={}, intento={}/{}: {}",
                    clothingId, attempt + 1, MAX_RETRIES + 1, e.getMessage()
                );
                // continuar al siguiente intento
            }
            // Cualquier otra excepción (RuntimeException de onStatus isError, o error de red)
            // se propaga inmediatamente sin reintentar.
        }

        // Todos los intentos agotados con errores reintentables.
        throw new RuntimeException(
            "dressme-ai no disponible tras " + (MAX_RETRIES + 1) + " intentos para prenda "
            + clothingId + ". Último error: " + lastRetryableError.getMessage(),
            lastRetryableError
        );
    }

    // ── Tipos internos ────────────────────────────────────────────────────────

    /**
     * Señal interna que indica un error transitorio del proveedor AI (503/429).
     * Diferencia los errores reintentables de los errores deterministas en el loop.
     * No sale del scope de AsyncVisionService.
     */
    private static class RetryableAiException extends RuntimeException {
        RetryableAiException(String message) {
            super(message);
        }
    }

    private record VisionAnalysisRequest(UUID clothingId, String imageUrl) {}
}