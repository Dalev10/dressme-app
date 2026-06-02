package com.dressme.dressme_database.controller;

import com.dressme.dressme_database.schema.dto.DressCodeDTO;
import com.dressme.dressme_database.service.DressCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/dress-codes")
@RequiredArgsConstructor
@Slf4j
public class InternalDressCodeController {

    private final DressCodeService dressCodeService;

    @GetMapping
    public ResponseEntity<List<DressCodeDTO>> getActiveDressCodes() {
        log.info("Database: Solicitud de dress codes activos");
        return ResponseEntity.ok(dressCodeService.getActiveDressCodes());
    }

    @GetMapping("/{id}/compatible")
    public ResponseEntity<List<UUID>> getCompatibleDressCodeIds(@PathVariable UUID id) {
        log.info("Database: Solicitud de compatibles para dress code {}", id);
        return ResponseEntity.ok(dressCodeService.getCompatibleDressCodeIds(id));
    }
}