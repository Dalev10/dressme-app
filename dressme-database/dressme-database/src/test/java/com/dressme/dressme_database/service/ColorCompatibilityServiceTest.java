package com.dressme.dressme_database.service;

import com.dressme.dressme_database.repository.ColorCompatibilityRepository;
import com.dressme.dressme_database.service.impl.ColorCompatibilityServiceImpl;
import com.dressme.dressme_database.util.ColorTheoryUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ColorCompatibilityServiceTest {

    @Mock
    private ColorCompatibilityRepository colorCompatibilityRepository;

    @InjectMocks
    private ColorCompatibilityServiceImpl colorCompatibilityService;

    private UUID colorId1;
    private UUID colorId2;

    @BeforeEach
    void setUp() {
        colorId1 = UUID.randomUUID();
        colorId2 = UUID.randomUUID();
    }

    @Test
    @DisplayName("Cuando existe un par en BD, retorna el score almacenado")
    void whenPairExistsInDB_thenReturnsStoredScore() {
        double expectedScore = 0.93;
        when(colorCompatibilityRepository.findScoreByColorIds(colorId1, colorId2))
            .thenReturn(Optional.of(expectedScore));

        Optional<Double> result = colorCompatibilityService.getCompatibilityScore(colorId1, colorId2);

        assertTrue(result.isPresent());
        assertEquals(expectedScore, result.get());
    }

    @Test
    @DisplayName("Cuando NO existe el par en BD, retorna Optional vacío")
    void whenPairNotInDB_thenReturnsEmptyOptional() {
        when(colorCompatibilityRepository.findScoreByColorIds(any(), any()))
            .thenReturn(Optional.empty());

        Optional<Double> result = colorCompatibilityService.getCompatibilityScore(colorId1, colorId2);

        assertTrue(result.isEmpty());
    }
}
