package com.dressme.dressme_database.service.impl;

import com.dressme.dressme_database.exception.ColorNotFoundException;
import com.dressme.dressme_database.mapper.ColorMapper;
import com.dressme.dressme_database.model.Color;
import com.dressme.dressme_database.repository.ColorRepository;
import com.dressme.dressme_database.schema.dto.ColorResponseDTO;
import com.dressme.dressme_database.service.ColorService;
import com.dressme.dressme_database.util.ColorTheoryUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ColorServiceImpl implements ColorService {
    private final ColorRepository colorRepository;
    private final ColorMapper colorMapper;

    @Override
    public ColorResponseDTO getComplementaryColor(UUID baseColorId){
        Color baseColor = getBaseColorOrThrow(baseColorId);

        if (baseColor.isNeutral()) return null;

        int targetHue = ColorTheoryUtils.getComplementaryHue(baseColor.getHue());

        return colorRepository.findByNeutralFalse().stream()
            .filter(color -> ColorTheoryUtils.isWithinTolerance(color.getHue(), targetHue))
            .findFirst()
            .map(colorMapper::toDto)
            .orElse(null);
    }

    @Override
    public List<ColorResponseDTO> getAnalogousColors(UUID baseColorId) {
        Color baseColor = getBaseColorOrThrow(baseColorId);

        if (baseColor.isNeutral()) return List.of();

        int[] analogousHues = ColorTheoryUtils.getAnalogousHues(baseColor.getHue());

        List<Color> analogousColors = colorRepository.findByNeutralFalse().stream()
            .filter(color -> ColorTheoryUtils.isWithinTolerance(color.getHue(), analogousHues[0]) ||
                ColorTheoryUtils.isWithinTolerance(color.getHue(), analogousHues[1]))
            .toList();

        return colorMapper.toDtoList(analogousColors);
    }

    private Color getBaseColorOrThrow(UUID id) {
        return colorRepository.findById(id)
            .orElseThrow(() -> new ColorNotFoundException("Color no encontrado con ID: " + id));
    }


}
