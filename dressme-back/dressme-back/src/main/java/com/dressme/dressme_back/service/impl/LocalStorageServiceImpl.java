package com.dressme.dressme_back.service.impl;

import com.dressme.dressme_back.config.StorageProperties;
import com.dressme.dressme_back.service.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Implementación de StorageService que guarda imágenes en el volumen Docker local.
 *
 * Estrategia:
 *   - dressme-back recibe el multipart y guarda la imagen bajo:
 *       /app/images/{userId}/{uuid}.{ext}
 *   - Genera una URL interna accesible desde cualquier contenedor en la red Docker:
 *       http://dressme-back:8080/images/{userId}/{uuid}.{ext}
 *   - El directorio /app/images está montado como volumen en docker-compose:
 *       ./wardrobe-images:/app/images
 *
 * Migración a GCS en producción:
 *   Solo hay que crear GcsStorageServiceImpl implementando la misma interfaz
 *   y activar el perfil "prod". dressme-back no cambia nada más.
 */
@Service
@Slf4j
public class LocalStorageServiceImpl implements StorageService {

    private final StorageProperties storageProperties;

    /**
     * Constructor que inyecta StorageProperties configuradas desde application.yml:
     *   storage.path: /app/images
     *   storage.base-url: http://dressme-back:8080/images
     */
    public LocalStorageServiceImpl(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    /**
     * Guarda el archivo multipart bajo {storagePath}/{userId}/{uuid}.{ext}
     * y devuelve la URL interna completa.
     *
     * @param userId    UUID del usuario propietario de la prenda.
     * @param file      Archivo de imagen recibido del cliente.
     * @return URL interna accesible desde la red Docker.
     * @throws RuntimeException si el archivo está vacío, tiene tipo no soportado,
     *                          o falla la escritura en disco.
     */
    @Override
    public String store(UUID userId, MultipartFile file) {
        validateFile(file);

        String extension  = resolveExtension(file.getOriginalFilename());
        String filename   = UUID.randomUUID() + "." + extension;
        Path   targetDir  = Paths.get(storageProperties.getPath(), userId.toString());
        Path   targetFile = targetDir.resolve(filename);

        try {
            Files.createDirectories(targetDir);
            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);
            log.info("Storage: Imagen guardada en {}", targetFile);
        } catch (IOException e) {
            log.error("Storage: Error al guardar imagen para usuario {}: {}", userId, e.getMessage());
            throw new RuntimeException("Error al guardar la imagen en el servidor", e);
        }

        // URL interna: http://dressme-back:8080/images/{userId}/{filename}
        return storageProperties.getBaseUrl() + "/" + userId + "/" + filename;
    }

    /**
     * Elimina el archivo físico dado su URL interna.
     * Usado cuando el usuario borra una prenda.
     *
     * @param imageUrl URL interna generada por store().
     */
    @Override
    public void delete(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return;

        // Extraer la ruta relativa: eliminar el prefijo base de la URL
        String relativePath = imageUrl.replace(storageProperties.getBaseUrl() + "/", "");
        Path   filePath     = Paths.get(storageProperties.getPath(), relativePath);

        try {
            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                log.info("Storage: Imagen eliminada: {}", filePath);
            } else {
                log.warn("Storage: Imagen no encontrada para eliminar: {}", filePath);
            }
        } catch (IOException e) {
            // No se lanza excepción: una imagen huérfana no debe bloquear la operación
            log.error("Storage: Error al eliminar imagen {}: {}", filePath, e.getMessage());
        }
    }

    // ── Helpers privados ──────────────────────────────────────────────────────

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("El archivo de imagen está vacío o no fue enviado");
        }

        String contentType = file.getContentType();
        if (contentType == null ||
                (!contentType.equals("image/jpeg") &&
                 !contentType.equals("image/png")  &&
                 !contentType.equals("image/webp"))) {
            throw new RuntimeException(
                    "Tipo de archivo no soportado: " + contentType +
                    ". Use JPEG, PNG o WebP.");
        }
    }

    private String resolveExtension(String originalFilename) {
        if (originalFilename != null && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
        }
        return "jpg"; // fallback
    }
}