package edu.ap.bib_backend.mapper;

import org.mapstruct.Mapper;
import java.util.List;

import edu.ap.bib_backend.dto.GenreDTO;
import edu.ap.bib_backend.jpa.entity.Genre;
import edu.ap.bib_backend.projection.GenreProjection;

@Mapper(componentModel = "spring")
public interface GenreMapper {

    GenreDTO toDto(GenreProjection genre);

    Genre toEntity(GenreDTO dto);

    List<GenreDTO> toDtoList(List<Genre> genres);

}