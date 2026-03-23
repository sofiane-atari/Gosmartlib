package edu.ap.bib_backend.service;

import edu.ap.bib_backend.dto.GenreDTO;
import edu.ap.bib_backend.dto.GenreStatsDTO;
import edu.ap.bib_backend.dto.PageDTO;
import edu.ap.bib_backend.exception.ResourceAlreadyExistsException;
import edu.ap.bib_backend.exception.ResourceNotFoundException;
import edu.ap.bib_backend.jpa.entity.Genre;
import edu.ap.bib_backend.jpa.repository.GenreRepository;
import edu.ap.bib_backend.mapper.GenreMapper;
import edu.ap.bib_backend.options.GenreGetOptions;
import edu.ap.bib_backend.projection.GenreProjection;
import edu.ap.bib_backend.projection.GenreWithCount;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GenreServiceTest {

        @Mock
        private GenreRepository genreRepository;

        @Mock
        private GenreMapper genreMapper;

        @InjectMocks
        private GenreService genreService;

        // ==================== createGenre Tests ====================

        @Test
        void givenNullName_whenCreateGenre_thenThrowException() {
                // Arrange
                GenreDTO dto = GenreDTO.builder().name(null).color("#FF0000").description("Some description").build();
                Genre entity = new Genre(null, "#FF0000", "Some description");

                when(genreMapper.toEntity(dto)).thenReturn(entity);
                when(genreRepository.save(any(Genre.class)))
                                .thenThrow(new IllegalArgumentException("Genre name must not be null"));

                // Act
                Exception exception = assertThrows(IllegalArgumentException.class,
                                () -> genreService.createGenre(dto));

                // Assert
                assertEquals("Genre name must not be null", exception.getMessage());
                verify(genreRepository, times(1)).save(any(Genre.class));
        }

        @Test
        void givenEmptyName_whenCreateGenre_thenThrowException() {
                // Arrange
                GenreDTO dto = GenreDTO.builder().name("").color("#FF0000").description("Some description").build();
                Genre entity = new Genre("", "#FF0000", "Some description");

                when(genreMapper.toEntity(dto)).thenReturn(entity);
                when(genreRepository.save(any(Genre.class)))
                                .thenThrow(new IllegalArgumentException("Genre name must not be empty"));

                // Act
                Exception exception = assertThrows(IllegalArgumentException.class,
                                () -> genreService.createGenre(dto));

                // Assert
                assertEquals("Genre name must not be empty", exception.getMessage());
                verify(genreRepository, times(1)).save(any(Genre.class));
        }

        @Test
        void givenExistingGenre_whenCreateGenre_thenThrowException() {
                // Arrange
                GenreDTO dto = GenreDTO.builder().name("Fiction").color("#FF0000").description("Fiction books").build();

                when(genreRepository.existsById("Fiction")).thenReturn(true);

                // Act
                assertThrows(ResourceAlreadyExistsException.class,
                                () -> genreService.createGenre(dto));

                // Assert
                verify(genreRepository, never()).save(any(Genre.class));
        }

        @Test
        void givenValidInput_whenCreateGenre_thenCreateSuccessfully() {
                // Arrange
                GenreDTO dto = GenreDTO.builder().name("Fiction").color("#FF0000").description("Fiction books").build();
                Genre entity = new Genre("Fiction", "#FF0000", "Fiction books");

                when(genreRepository.existsById("Fiction")).thenReturn(false);
                when(genreMapper.toEntity(dto)).thenReturn(entity);
                when(genreMapper.toDto(entity)).thenReturn(dto);

                // Act
                GenreDTO result = genreService.createGenre(dto);

                // Assert
                assertNotNull(result);
                assertEquals("Fiction", result.name());
                assertEquals("#FF0000", result.color());
                assertEquals("Fiction books", result.description());
                verify(genreRepository, times(1)).save(entity);
        }

        @Test
        void givenInvalidRequest_whenCreateGenre_thenThrowException() {
                // Arrange
                GenreDTO dto = GenreDTO.builder().build();
                Genre entity = new Genre(null, null, null);

                when(genreMapper.toEntity(dto)).thenReturn(entity);
                when(genreRepository.save(any(Genre.class)))
                                .thenThrow(new ConstraintViolationException("Validation failed", null));

                // Act
                assertThrows(ConstraintViolationException.class,
                                () -> genreService.createGenre(dto));

                // Assert
                verify(genreRepository, times(1)).save(any(Genre.class));
        }

        // ==================== getPage Tests ====================

        @Test
        void givenNoOptions_whenGetPage_thenReturnAllGenres() {
                // Arrange
                int page = 0;
                int pageSize = 10;
                GenreGetOptions options = new GenreGetOptions();

                Genre genre = new Genre("Test", "#FF0000", "Test description");
                Page<Genre> genrePage = new PageImpl<>(List.of(genre),
                                org.springframework.data.domain.PageRequest.of(page, pageSize), 1);

                when(genreRepository.findAll(any(Pageable.class))).thenReturn(genrePage);
                when(genreMapper.toDto(any(Genre.class))).thenReturn(
                                GenreDTO.builder().name("Test").color("#FF0000").description("Test desc").build());

                // Act
                PageDTO<GenreDTO> result = genreService.getPage(page, pageSize, options);

                // Assert
                assertNotNull(result);
                assertNotNull(result.data());
                assertNotNull(result.meta());
                verify(genreRepository, times(1)).findAll(any(Pageable.class));
        }

        @Test
        void givenIncludeBookCountTrue_whenGetPage_thenUseFindAllFiltered() {
                // Arrange
                int page = 0;
                int pageSize = 10;
                GenreGetOptions options = new GenreGetOptions();
                options.setIncludeBookCount(true);

                GenreWithCount projection = mock(GenreWithCount.class);
                Page<GenreWithCount> genrePage = new PageImpl<>(List.of(projection),
                                org.springframework.data.domain.PageRequest.of(page, pageSize), 1);

                when(genreRepository.findAllFiltered(any(), any(), any(Pageable.class))).thenReturn(genrePage);
                when(genreMapper.toDto(any(GenreProjection.class))).thenReturn(
                                GenreDTO.builder().name("Test").color("#FF0000").description("Test desc").build());

                // Act
                PageDTO<GenreDTO> result = genreService.getPage(page, pageSize, options);

                // Assert
                assertNotNull(result);
                verify(genreRepository, times(1)).findAllFiltered(any(), any(), any(Pageable.class));
                verify(genreRepository, never()).findAll(any(Pageable.class));
                verify(genreRepository, never()).searchByNamerOrDescriptionOrColor(any(), any(Pageable.class));
        }

        @Test
        void givenSearchQuery_whenGetPage_thenUseSearch() {
                // Arrange
                int page = 0;
                int pageSize = 10;
                GenreGetOptions options = new GenreGetOptions();
                options.setSearch("fiction");

                Genre genre = new Genre("Fiction", "#FF0000", "Fiction books");
                Page<Genre> genrePage = new PageImpl<>(List.of(genre),
                                org.springframework.data.domain.PageRequest.of(page, pageSize), 1);

                when(genreRepository.searchByNamerOrDescriptionOrColor(eq("fiction"), any(Pageable.class)))
                                .thenReturn(genrePage);
                when(genreMapper.toDto(any(Genre.class))).thenReturn(
                                GenreDTO.builder().name("Fiction").color("#FF0000").description("Fiction books")
                                                .build());

                // Act
                PageDTO<GenreDTO> result = genreService.getPage(page, pageSize, options);

                // Assert
                assertNotNull(result);
                verify(genreRepository, times(1)).searchByNamerOrDescriptionOrColor(eq("fiction"), any(Pageable.class));
                verify(genreRepository, never()).findAllFiltered(any(), any(), any(Pageable.class));
        }

        @Test
        void givenBookCountFilter_whenGetPage_thenUseFindAllFiltered() {
                // Arrange
                int page = 0;
                int pageSize = 10;
                GenreGetOptions options = new GenreGetOptions();
                options.setBookCount(1);

                GenreWithCount projection = mock(GenreWithCount.class);
                Page<GenreWithCount> genrePage = new PageImpl<>(List.of(projection),
                                org.springframework.data.domain.PageRequest.of(page, pageSize), 1);

                when(genreRepository.findAllFiltered(any(), eq(1), any(Pageable.class))).thenReturn(genrePage);
                when(genreMapper.toDto(any(GenreProjection.class))).thenReturn(
                                GenreDTO.builder().name("Test").color("#FF0000").description("Test desc").build());

                // Act
                PageDTO<GenreDTO> result = genreService.getPage(page, pageSize, options);

                // Assert
                assertNotNull(result);
                verify(genreRepository, times(1)).findAllFiltered(any(), eq(1), any(Pageable.class));
                verify(genreRepository, never()).searchByNamerOrDescriptionOrColor(any(), any(Pageable.class));
        }

        // ==================== getAllGenreStats Tests ====================

        @Test
        void givenGenresExist_whenGetAllGenreStats_thenReturnCorrectStats() {
                // Arrange
                GenreWithCount genre1 = mock(GenreWithCount.class);
                GenreWithCount genre2 = mock(GenreWithCount.class);
                GenreWithCount genre3 = mock(GenreWithCount.class);

                when(genre1.getBookCount()).thenReturn(5);
                when(genre2.getBookCount()).thenReturn(0);
                when(genre3.getBookCount()).thenReturn(0);

                List<GenreWithCount> genres = List.of(genre1, genre2, genre3);
                Page<GenreWithCount> genrePage = new PageImpl<>(genres);

                when(genreRepository.findAllIncludeBookCount(any())).thenReturn(genrePage);

                // Act
                GenreStatsDTO result = genreService.getAllGenreStats();

                // Assert
                assertNotNull(result);
                assertEquals(3, result.totalItems());
                assertEquals(2, result.unActiveGenres());
                verify(genreRepository, times(1)).findAllIncludeBookCount(any());
        }

        @Test
        void givenNoGenres_whenGetAllGenreStats_thenReturnZeroStats() {
                // Arrange
                Page<GenreWithCount> emptyPage = new PageImpl<>(Collections.emptyList());

                when(genreRepository.findAllIncludeBookCount(any())).thenReturn(emptyPage);

                // Act
                GenreStatsDTO result = genreService.getAllGenreStats();

                // Assert
                assertNotNull(result);
                assertEquals(0, result.totalItems());
                assertEquals(0, result.unActiveGenres());
                verify(genreRepository, times(1)).findAllIncludeBookCount(any());
        }

        @Test
        void givenAllGenresHaveBooks_whenGetAllGenreStats_thenReturnCorrectStats() {
                // Arrange
                GenreWithCount genre1 = mock(GenreWithCount.class);
                GenreWithCount genre2 = mock(GenreWithCount.class);

                when(genre1.getBookCount()).thenReturn(10);
                when(genre2.getBookCount()).thenReturn(5);

                List<GenreWithCount> genres = List.of(genre1, genre2);
                Page<GenreWithCount> genrePage = new PageImpl<>(genres);

                when(genreRepository.findAllIncludeBookCount(any())).thenReturn(genrePage);

                // Act
                GenreStatsDTO result = genreService.getAllGenreStats();

                // Assert
                assertNotNull(result);
                assertEquals(2, result.totalItems());
                assertEquals(0, result.unActiveGenres());
                verify(genreRepository, times(1)).findAllIncludeBookCount(any());
        }

        // ==================== deleteById Tests ====================

        @Test
        void givenExistingGenreName_whenDeleteById_thenDeleteSuccessfully() {
                // Arrange
                String genreName = "Fiction";
                Genre genre = new Genre(genreName, "#FF0000", "Fiction books");

                when(genreRepository.findById(genreName)).thenReturn(Optional.of(genre));
                doNothing().when(genreRepository).deleteById(genreName);

                // Act
                genreService.deleteById(genreName);

                // Assert
                verify(genreRepository, times(1)).findById(genreName);
                verify(genreRepository, times(1)).deleteById(genreName);
        }

    @Test
        void givenNonExistingGenreName_whenDeleteById_thenThrowException() {
                // Arrange
                String genreName = "NonExistent";

                when(genreRepository.findById(genreName)).thenReturn(Optional.empty());

                // Act & Assert
                ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                                () -> genreService.deleteById(genreName));

                // Assert - message doesn't contain the genre name, it's a static Dutch message
                assertNotNull(exception.getMessage());
                verify(genreRepository, times(1)).findById(genreName);
                verify(genreRepository, never()).deleteById(any());
        }

        @Test
        void givenGenreWithBooks_whenDeleteById_thenDeleteSuccessfully() {
                // Arrange
                String genreName = "Science";
                Genre genre = new Genre(genreName, "#0000FF", "Science books");

                when(genreRepository.findById(genreName)).thenReturn(Optional.of(genre));
                doNothing().when(genreRepository).deleteById(genreName);

                // Act
                genreService.deleteById(genreName);

                // Assert
                verify(genreRepository, times(1)).findById(genreName);
                verify(genreRepository, times(1)).deleteById(genreName);
        }

        // ==================== update Tests ====================

        @Test
        void givenExistingGenre_whenUpdate_thenUpdateSuccessfully() {
                // Arrange
                String genreName = "Fiction";
                GenreDTO inputDTO = GenreDTO.builder()
                                .name(genreName)
                                .color("#00FF00")
                                .description("Updated fiction description")
                                .build();
                Genre existingGenre = new Genre(genreName, "#FF0000", "Original description");
                Genre updatedGenre = new Genre(genreName, "#00FF00", "Updated fiction description");

                when(genreRepository.findById(genreName)).thenReturn(Optional.of(existingGenre));
                when(genreRepository.save(any(Genre.class))).thenReturn(updatedGenre);
                when(genreMapper.toDto(updatedGenre)).thenReturn(inputDTO);

                // Act
                GenreDTO result = genreService.update(genreName, inputDTO);

                // Assert
                assertNotNull(result);
                assertEquals("#00FF00", result.color());
                assertEquals("Updated fiction description", result.description());
                verify(genreRepository, times(1)).findById(genreName);
                verify(genreRepository, times(1)).save(any(Genre.class));
                verify(genreMapper, times(1)).toDto(updatedGenre);
        }

        @Test
        void givenNonExistingGenre_whenUpdate_thenThrowException() {
                // Arrange
                String genreName = "NonExistent";
                GenreDTO inputDTO = GenreDTO.builder()
                                .name(genreName)
                                .color("#00FF00")
                                .description("Some description")
                                .build();

                when(genreRepository.findById(genreName)).thenReturn(Optional.empty());

                // Act
                ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                                () -> genreService.update(genreName, inputDTO));

                // Assert
                assertNotNull(exception.getMessage());
                verify(genreRepository, times(1)).findById(genreName);
                verify(genreRepository, never()).save(any(Genre.class));
        }

        @Test
        void givenValidInputWithAllFields_whenUpdate_thenUpdateAllFields() {
                // Arrange
                String genreName = "Science";
                GenreDTO inputDTO = GenreDTO.builder()
                                .name(genreName)
                                .color("#0000FF")
                                .description("Science and technology books")
                                .build();
                Genre existingGenre = new Genre(genreName, "#FF0000", "Old description");
                Genre savedGenre = new Genre(genreName, "#0000FF", "Science and technology books");

                when(genreRepository.findById(genreName)).thenReturn(Optional.of(existingGenre));
                when(genreRepository.save(any(Genre.class))).thenReturn(savedGenre);
                when(genreMapper.toDto(savedGenre)).thenReturn(inputDTO);

                // Act
                GenreDTO result = genreService.update(genreName, inputDTO);

                // Assert
                assertNotNull(result);
                assertEquals("#0000FF", result.color());
                assertEquals("Science and technology books", result.description());
                verify(genreRepository, times(1)).findById(genreName);
                verify(genreRepository, times(1)).save(existingGenre);
        }

        @Test
        void givenUpdateOnlyColor_whenUpdate_thenUpdateColorOnly() {
                // Arrange
                String genreName = "Fiction";
                GenreDTO inputDTO = GenreDTO.builder()
                                .name(genreName)
                                .color("#FFFF00")
                                .description(null)
                                .build();
                Genre existingGenre = new Genre(genreName, "#FF0000", "Original description");
                Genre savedGenre = new Genre(genreName, "#FFFF00", "Original description");

                when(genreRepository.findById(genreName)).thenReturn(Optional.of(existingGenre));
                when(genreRepository.save(any(Genre.class))).thenReturn(savedGenre);
                when(genreMapper.toDto(savedGenre)).thenReturn(
                                GenreDTO.builder().name(genreName).color("#FFFF00").description("Original description")
                                                .build());

                // Act
                GenreDTO result = genreService.update(genreName, inputDTO);

                // Assert
                assertNotNull(result);
                verify(genreRepository, times(1)).findById(genreName);
                verify(genreRepository, times(1)).save(any(Genre.class));
        }
}
