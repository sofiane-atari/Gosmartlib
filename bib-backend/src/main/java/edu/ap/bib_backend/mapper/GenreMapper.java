package edu.ap.bib_backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import java.util.List;

import edu.ap.bib_backend.dto.GenreDTO;
import edu.ap.bib_backend.jpa.entity.Genre;
import edu.ap.bib_backend.projection.GenreProjection;

@Mapper(componentModel = "spring")
public interface GenreMapper {

    GenreDTO toDto(Genre genre);

    @Mapping(target = "subGenres", ignore = true)
    GenreDTO toDto(GenreProjection genre);

    @Mapping(target = "books", ignore = true)
    @Mapping(target = "parentGenre", ignore = true)
    Genre toEntity(GenreDTO dto);

    List<GenreDTO> toDtoList(List<Genre> genres);

}