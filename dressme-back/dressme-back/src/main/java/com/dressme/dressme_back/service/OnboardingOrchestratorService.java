package com.dressme.dressme_back.service;

import com.dressme.dressme_back.schema.dto.OnboardingCalibrationResponse;
import com.dressme.dressme_back.schema.dto.OnboardingSelectionRequest;
import com.dressme.dressme_back.schema.dto.StyleCardDTO;

import java.util.List;

public interface OnboardingOrchestratorService {

    List<StyleCardDTO> getStyleCards();

    OnboardingCalibrationResponse calibrate(String secureUserId, OnboardingSelectionRequest request);
}
