package edu.ap.bib_backend.mapper;

import edu.ap.bib_backend.dto.didactic.DidacticMaterialBookSummaryResponseDto;
import edu.ap.bib_backend.dto.didactic.DidacticMaterialResponseDto;
import edu.ap.bib_backend.jpa.entity.didactic.DidacticMaterial;
import edu.ap.bib_backend.jpa.entity.didactic.DidacticMaterialBookLink;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DidacticMaterialMapper {

    @Mapping(target = "linkedBooks", source = "bookLinks")
    DidacticMaterialResponseDto toResponse(DidacticMaterial didacticMaterial);

    List<DidacticMaterialResponseDto> toResponseList(List<DidacticMaterial> didacticMaterials);

    @Mapping(target = "id", source = "book.id")
    @Mapping(target = "title", source = "book.title")
    @Mapping(target = "author", source = "book.author")
    DidacticMaterialBookSummaryResponseDto toBookSummaryResponse(DidacticMaterialBookLink bookLink);
}