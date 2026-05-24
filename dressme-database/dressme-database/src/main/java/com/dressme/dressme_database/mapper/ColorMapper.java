package com.dressme.dressme_database.mapper;

import com.dressme.dressme_database.model.Color;
import com.dressme.dressme_database.schema.dto.ColorResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ColorMapper {
    ColorResponseDTO toDto(Color color);
    List<ColorResponseDTO> toDtoList(List<Color> colors);
}
