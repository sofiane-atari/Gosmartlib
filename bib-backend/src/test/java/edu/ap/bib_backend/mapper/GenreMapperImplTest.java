// package edu.ap.bib_backend.mapper;

// import static org.assertj.core.api.Assertions.assertThat;
// import static org.mockito.Mockito.mock;
// import static org.mockito.Mockito.when;

// import java.util.List;

// import org.junit.jupiter.api.Test;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.context.SpringBootTest;

// import edu.ap.bib_backend.dto.GenreDTO;
// import edu.ap.bib_backend.jpa.entity.Genre;
// import edu.ap.bib_backend.projection.GenreProjection;

// @SpringBootTest(classes = GenreMapperImpl.class)
// class GenreMapperImplTest {

//     @Autowired
//     private GenreMapperImpl genreMapper;

//     @Test
//     void givenGenreProjection_whenToDto_thenMapsProjectionToDto() {
//         // Arrange
//         GenreProjection projection = mock(GenreProjection.class);
//         when(projection.getName()).thenReturn("Fantasy");
//         when(projection.getColor()).thenReturn("#FF5733");
//         when(projection.getDescription()).thenReturn("Draken en magie");
//         when(projection.getBookCount()).thenReturn(10);

//         // Act
//         GenreDTO result = genreMapper.toDto(projection);

//         // Assert
//         assertThat(result).isNotNull();
//         assertThat(result.name()).isEqualTo("Fantasy");
//         assertThat(result.color()).isEqualTo("#FF5733");
//         assertThat(result.description()).isEqualTo("Draken en magie");
//         assertThat(result.bookCount()).isEqualTo(10);
//     }

//     @Test
//     void givenGenreDto_whenToEntity_thenMapsDtoToEntity() {
//         // Arrange
//         GenreDTO dto = GenreDTO.builder()
//                 .name("Horror")
//                 .color("#000000")
//                 .description("Griezelig")
//                 .build();

//         // Act
//         Genre result = genreMapper.toEntity(dto);

//         // Assert
//         assertThat(result).isNotNull();
//         assertThat(result.getName()).isEqualTo("Horror");
//         assertThat(result.getColor()).isEqualTo("#000000");
//         assertThat(result.getDescription()).isEqualTo("Griezelig");
//     }

//     @Test
//     void givenGenreList_whenToDtoList_thenMapsListCorrectly() {
//         // Arrange
//         Genre genre = new Genre();
//         genre.setName("Sci-Fi");
//         List<Genre> genres = List.of(genre);

//         // Act
//         List<GenreDTO> result = genreMapper.toDtoList(genres);

//         // Assert
//         assertThat(result).hasSize(1);
//         assertThat(result.get(0).name()).isEqualTo("Sci-Fi");
//     }

//     @Test
//     void givenNullProjection_whenToDto_thenReturnsNull() {
//         // Arrange
//         GenreProjection projection = null;

//         // Act
//         GenreDTO result = genreMapper.toDto(projection);

//         // Assert
//         assertThat(result).isNull();
//     }

//     @Test
//     void givenNullDto_whenToEntity_thenReturnsNull() {
//         // Arrange
//         GenreDTO dto = null;

//         // Act
//         Genre result = genreMapper.toEntity(dto);

//         // Assert
//         assertThat(result).isNull();
//     }

//     @Test
//     void givenNullGenreList_whenToDtoList_thenReturnsNull() {
//         // Arrange
//         List<Genre> genres = null;

//         // Act
//         List<GenreDTO> result = genreMapper.toDtoList(genres);

//         // Assert
//         assertThat(result).isNull();
//     }
// }