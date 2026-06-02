package com.dressme.dressme_back.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/debug")
@Slf4j
public class DebugController {

    @GetMapping("/jwt-check")
    public ResponseEntity<Map<String, Object>> checkJwt(
        @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        Map<String, Object> response = new HashMap<>();
        
        log.info("DEBUG: Authorization header recibido: {}", authHeader != null ? "present" : "missing");
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("DEBUG: Security context authentication: {}", auth);
        
        response.put("authHeader", authHeader);
        if (auth != null) {
            response.put("principal", auth.getPrincipal());
            response.put("authorities", auth.getAuthorities());
            response.put("authenticated", auth.isAuthenticated());
        } else {
            response.put("authentication", "null");
        }
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/internal-debug")
    public ResponseEntity<Map<String, Object>> debugInternal(
        @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        Map<String, Object> response = new HashMap<>();
        
        log.info("DEBUG /internal-debug: Authorization header: {}", authHeader != null ? "present" : "missing");
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("DEBUG /internal-debug: Authentication: {}", auth);
        if (auth != null) {
            log.info("DEBUG /internal-debug: Authorities: {}", auth.getAuthorities());
        }
        
        response.put("authHeader", authHeader != null ? "present" : "missing");
        response.put("authentication", auth != null ? auth.getPrincipal() : "null");
        response.put("authorities", auth != null ? auth.getAuthorities().toString() : "none");
        
        return ResponseEntity.ok(response);
    }
}
