// ─── ClothingUploadRequest.java ──────────────────────────────────────────────
// Lo que llega del Gateway: userId + el archivo multipart.
// El archivo NO va en el JSON — el controller lo recibe con @RequestPart.
// Este DTO transporta solo la parte estructurada de la petición.

package com.dressme.dressme_back.schema.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ClothingUploadRequest(

    @NotNull(message = "El userId es obligatorio")
    UUID userId

) {}