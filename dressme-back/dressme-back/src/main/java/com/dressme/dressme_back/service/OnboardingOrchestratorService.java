package com.dressme.dressme_back.service;
 
import com.dressme.dressme_back.schema.dto.OnboardingCalibrationResponse;
import com.dressme.dressme_back.schema.dto.OnboardingSelectionRequest;
import com.dressme.dressme_back.schema.dto.StyleCardDTO;
 
import java.util.List;
 
public interface OnboardingOrchestratorService {
 
    /**
     * Devuelve las style cards activas para la pantalla de onboarding.
     * Consulta dressme-database y reenvía la lista al Gateway → Frontend.
     */
    List<StyleCardDTO> getStyleCards();
 
    /**
     * Orquesta el flujo completo de calibración del taste vector:
     *   1. Obtiene los embeddings de las tarjetas seleccionadas (dressme-database)
     *   2. Calcula el taste vector (dressme-ai)
     *   3. Persiste el vector y marca isCalibrated=true (dressme-database)
     *   4. Persiste las selecciones del usuario (dressme-database)
     */
    OnboardingCalibrationResponse calibrate(OnboardingSelectionRequest request);
}