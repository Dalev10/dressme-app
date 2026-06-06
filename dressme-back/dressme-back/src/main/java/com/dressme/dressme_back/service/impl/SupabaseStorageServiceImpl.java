package com.dressme.dressme_back.service.impl;

import com.dressme.dressme_back.config.SupabaseStorageProperties;
import com.dressme.dressme_back.service.StorageService;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implementación de StorageService que delega en Supabase Storage.
 *
 * Flujo de upload:
 *   1. dressme-back recibe el MultipartFile del gateway
 *   2. Sube los bytes directamente a Supabase via REST API
 *   3. Devuelve la URL pública permanente del objeto
 *
 * URL pública: {supabaseUrl}/storage/v1/object/public/{bucket}/{userId}/{filename}
 *
 * Esta URL es accesible desde cualquier browser sin autenticación
 * porque el bucket está configurado como público en Supabase Dashboard.
 *
 * Delete:
 *   Llama al endpoint batch de Supabase para eliminar el objeto
 *   por su path relativo dentro del bucket.
 */
@Service
@Slf4j
public class SupabaseStorageServiceImpl implements StorageService {

    private final SupabaseStorageProperties props;
    private final RestClient storageClient;

    public SupabaseStorageServiceImpl(SupabaseStorageProperties props) {
        if (props.getUrl() == null || props.getUrl().isBlank()) {
            throw new IllegalStateException("SUPABASE_URL must be set");
        }
        if (props.getServiceKey() == null || props.getServiceKey().isBlank()) {
            throw new IllegalStateException("SUPABASE_SERVICE_KEY must be set");
        }
        if (props.getBucket() == null || props.getBucket().isBlank()) {
            throw new IllegalStateException("SUPABASE_BUCKET must be set");
        }

        this.props = props;

        // JdkClientHttpRequestFactory para evitar conflictos con OkHttp/Feign en classpath
        this.storageClient = RestClient.builder()
                .requestFactory(new JdkClientHttpRequestFactory())
                .baseUrl(props.getUrl())
                .defaultHeader("Authorization", "Bearer " + props.getServiceKey())
                .defaultHeader("apikey", props.getServiceKey())
                .build();

        log.info("SupabaseStorage: bucket={} url={}", props.getBucket(), props.getUrl());
    }

    @Override
    public String store(UUID userId, MultipartFile file) {
        validateFile(file);

        String extension = resolveExtension(file.getOriginalFilename());
        String filename  = UUID.randomUUID() + "." + extension;
        String objectPath = userId + "/" + filename;

        try {
            // POST /storage/v1/object/{bucket}/{userId}/{filename}
            // El path se construye como string para evitar que el template URI codifique las barras
            storageClient.post()
                    .uri("/storage/v1/object/" + props.getBucket() + "/" + objectPath)
                    .contentType(MediaType.parseMediaType(file.getContentType()))
                    .header("x-upsert", "true")
                    .body(file.getBytes())
                    .retrieve()
                    .toBodilessEntity();

        } catch (IOException e) {
            throw new RuntimeException("Error al leer el archivo para subir a Supabase", e);
        }

        String publicUrl = props.getUrl()
                + "/storage/v1/object/public/"
                + props.getBucket() + "/"
                + objectPath;

        log.info("Storage: imagen subida a Supabase → {}", publicUrl);
        return publicUrl;
    }

    @Override
    public void delete(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return;

        String publicPrefix = props.getUrl()
                + "/storage/v1/object/public/"
                + props.getBucket() + "/";

        if (!imageUrl.startsWith(publicPrefix)) {
            // URL de formato antiguo (http://dressme-back:8080/images/...) — ignorar silenciosamente
            log.warn("Storage: URL de formato no reconocido, se omite la eliminación: {}", imageUrl);
            return;
        }

        String objectPath = imageUrl.substring(publicPrefix.length());

        // DELETE /storage/v1/object/{bucket} con body {"prefixes": ["objectPath"]}
        storageClient.method(HttpMethod.DELETE)
                .uri("/storage/v1/object/" + props.getBucket())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("prefixes", List.of(objectPath)))
                .retrieve()
                .toBodilessEntity();

        log.info("Storage: imagen eliminada de Supabase → {}", objectPath);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("El archivo de imagen está vacío o no fue enviado");
        }
        String contentType = file.getContentType();
        if (contentType == null ||
                (!contentType.equals("image/jpeg") &&
                 !contentType.equals("image/png")  &&
                 !contentType.equals("image/webp"))) {
            throw new RuntimeException("Tipo de archivo no soportado: " + contentType);
        }
    }

    private String resolveExtension(String originalFilename) {
        if (originalFilename != null && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
        }
        return "jpg";
    }
}
