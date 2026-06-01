package com.dressme.dressme_back.service;

import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;

/**
 * Abstracción del servicio de almacenamiento de imágenes.
 *
 * Implementaciones:
 *   - LocalStorageServiceImpl  → volumen Docker (MVP)
 *   - GcsStorageServiceImpl    → Google Cloud Storage (producción)
 *
 * Para cambiar de implementación en producción basta con:
 *   1. Crear GcsStorageServiceImpl anotado con @Profile("prod")
 *   2. Anotar LocalStorageServiceImpl con @Profile("dev")
 *   No se modifica ningún otro archivo.
 */
public interface StorageService {

    /**
     * Guarda el archivo y devuelve su URL de acceso.
     *
     * @param userId propietario de la imagen.
     * @param file   multipart recibido del cliente.
     * @return URL interna (volumen local) o pública (GCS).
     */
    String store(UUID userId, MultipartFile file);

    /**
     * Elimina el archivo asociado a la URL dada.
     * No lanza excepción si el archivo ya no existe.
     *
     * @param imageUrl URL devuelta por store().
     */
    void delete(String imageUrl);
}