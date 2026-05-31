package com.dressme.dressme_back.controller;

import com.dressme.dressme_back.config.StorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Controlador para servir imágenes del wardrobe.
 *
 * Expone el endpoint:
 *   GET /images/{userId}/{filename}
 *
 * Usado por:
 *   - dressme-ai para descargar imágenes y analizarlas con Gemini Vision
 *   - Frontend para mostrar previewsSoftware
 *
 * La ruta física está configurada en StorageProperties (por defecto: /app/images)
 */
@RestController
@RequestMapping("/images")
@RequiredArgsConstructor
@Slf4j
public class ImageController {

    private final StorageProperties storageProperties;

    /**
     * Sirve una imagen del wardrobe.
     *
     * @param userId   UUID del usuario propietario de la prenda.
     * @param filename Nombre del archivo (ej: "abc123-def456.jpg").
     * @return La imagen con headers appropriate (Content-Type, Content-Disposition).
     * @throws RuntimeException si el archivo no existe o no es accesible.
     */
    @GetMapping("/{userId}/{filename}")
    public ResponseEntity<Resource> getImage(
            @PathVariable UUID userId,
            @PathVariable String filename) {

        try {
            log.info("ImageController: GET /images/{}/{}", userId, filename);

            // Construir la ruta física: /app/images/{userId}/{filename}
            Path imagePath = Paths.get(storageProperties.getPath(), userId.toString(), filename);

            // Validar que el archivo existe
            if (!Files.exists(imagePath)) {
                log.warn("ImageController: Imagen no encontrada: {}", imagePath);
                return ResponseEntity.notFound().build();
            }

            // Validar que el archivo está dentro del directorio permitido (evitar path traversal)
            if (!imagePath.normalize().startsWith(Paths.get(storageProperties.getPath()).normalize())) {
                log.warn("ImageController: Path traversal attempt detected: {}", imagePath);
                return ResponseEntity.badRequest().build();
            }

            // Cargar la imagen como Resource
            Resource resource = new UrlResource(imagePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                log.warn("ImageController: Imagen no es legible: {}", imagePath);
                return ResponseEntity.internalServerError().build();
            }

            // Detectar el tipo MIME
            String contentType = Files.probeContentType(imagePath);
            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            log.info("ImageController: Sirviendo imagen {} (tipo: {})", imagePath, contentType);

            // Devolver la imagen con headers apropriados
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(resource);

        } catch (MalformedURLException e) {
            log.error("ImageController: Error de URL: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        } catch (Exception e) {
            log.error("ImageController: Error inesperado al servir imagen {}/{}: {}", 
                    userId, filename, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}
