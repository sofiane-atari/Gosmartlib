package edu.ap.bib_backend.mapper.didactic;

import edu.ap.bib_backend.dto.didactic.DidacticMaterialBookSummaryResponseDto;
import edu.ap.bib_backend.dto.didactic.DidacticMaterialResponseDto;
import edu.ap.bib_backend.jpa.entity.didactic.DidacticMaterial;
import edu.ap.bib_backend.jpa.entity.didactic.DidacticMaterialBookLink;
import edu.ap.bib_backend.jpa.enums.Role;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(componentModel = "spring")
public interface DidacticMaterialMapper {

    @Mapping(target = "linkedBooks", source = "didacticMaterial.bookLinks")
    @Mapping(target = "createdByRole", source = "createdByRole")
    DidacticMaterialResponseDto toResponse(DidacticMaterial didacticMaterial, Role createdByRole);

    @Mapping(target = "id", source = "book.id")
    @Mapping(target = "title", source = "book.title")
    @Mapping(target = "author", source = "book.author")
    @Mapping(target = "imageUrl", source = "book.imageUrl")
    DidacticMaterialBookSummaryResponseDto toBookSummaryResponse(DidacticMaterialBookLink bookLink);
}