package com.dressme.dressme_gateway.controller;

import com.dressme.dressme_gateway.schema.dto.UserResponseDTO;
import com.dressme.dressme_gateway.schema.dto.UserUpdateRequest;
import com.dressme.dressme_gateway.service.UserService;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;


@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    @GetMapping("/profile/{id}")
    public ResponseEntity<UserResponseDTO> getUserProfile(@PathVariable UUID id) {
        log.info("Gateway-Controller: Recibida petición pública de perfil para ID: {}", id);
        return ResponseEntity.ok(userService.getUserProfile(id));
    }

    @PatchMapping("/profile/{id}")
    public ResponseEntity<UserResponseDTO> update(
            @PathVariable UUID id, 
            @RequestBody UserUpdateRequest request) {
        
        if (request.getDisplayName() == null && request.getIsCalibrated() == null) {
            log.error("GATEWAY CRITICAL: Jackson falló al mapear el JSON. Campos recibidos como NULL.");
        }

        log.info("GATEWAY DEBUG FINAL: ID={}, Name={}, Calib={}", 
                id, request.getDisplayName(), request.getIsCalibrated());
                
        return ResponseEntity.ok(userService.updateUserProfile(id, request));
    }

    @DeleteMapping("/profile/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        log.info("Gateway-Controller: Ejecutando DELETE público para usuario: {}", id);
        userService.deleteUserProfile(id);
        return ResponseEntity.noContent().build();
    }
}