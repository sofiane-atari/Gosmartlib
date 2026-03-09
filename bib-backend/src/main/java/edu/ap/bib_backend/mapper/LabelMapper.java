package edu.ap.bib_backend.mapper;

import org.mapstruct.Mapper;
import java.util.List;

import edu.ap.bib_backend.dto.LabelDTO;
import edu.ap.bib_backend.jpa.entity.Label;
import edu.ap.bib_backend.projection.LabelProjection;

@Mapper(componentModel = "spring")
public interface LabelMapper {

    LabelDTO toDto(LabelProjection label);

    Label toEntity(LabelDTO dto);

    List<LabelDTO> toDtoList(List<Label> labels);

}