package edu.ap.bib_backend;

import edu.ap.bib_backend.dto.GenreDTO;
import edu.ap.bib_backend.dto.GenreStatsDTO;
import edu.ap.bib_backend.dto.PageDTO;
import edu.ap.bib_backend.exception.ResourceAlreadyExistsException;
import edu.ap.bib_backend.exception.ResourceNotFoundException;
import edu.ap.bib_backend.jpa.entity.Genre;
import edu.ap.bib_backend.jpa.repository.GenreRepository;
import edu.ap.bib_backend.mapper.GenreMapper;
import edu.ap.bib_backend.options.LabelGetOptions;
import edu.ap.bib_backend.projection.GenreProjection;
import edu.ap.bib_backend.projection.GenreWithCount;
import edu.ap.bib_backend.service.GenreService;
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
class LabelServiceTest {

        @Mock
        private GenreRepository labelRepository;

        @Mock
        private GenreMapper genreMapper;

        @InjectMocks
        private GenreService labelService;

        // ==================== createLabel Tests ====================

        @Test
        void givenNullName_whenCreateLabel_thenThrowException() {
                // Arrange
                GenreDTO dto = GenreDTO.builder().name(null).color("#FF0000").description("Some description").build();
                Genre entity = new Genre(null, "#FF0000", "Some description");

                when(genreMapper.toEntity(dto)).thenReturn(entity);
                when(labelRepository.save(any(Genre.class)))
                                .thenThrow(new IllegalArgumentException("Label name must not be null"));

                // Act
                Exception exception = assertThrows(IllegalArgumentException.class,
                                () -> labelService.createLabel(dto));

                // Assert
                assertEquals("Label name must not be null", exception.getMessage());
                verify(labelRepository, times(1)).save(any(Genre.class));
        }

        @Test
        void givenEmptyName_whenCreateLabel_thenThrowException() {
                // Arrange
                GenreDTO dto = GenreDTO.builder().name("").color("#FF0000").description("Some description").build();
                Genre entity = new Genre("", "#FF0000", "Some description");

                when(genreMapper.toEntity(dto)).thenReturn(entity);
                when(labelRepository.save(any(Genre.class)))
                                .thenThrow(new IllegalArgumentException("Label name must not be empty"));

                // Act
                Exception exception = assertThrows(IllegalArgumentException.class,
                                () -> labelService.createLabel(dto));

                // Assert
                assertEquals("Label name must not be empty", exception.getMessage());
                verify(labelRepository, times(1)).save(any(Genre.class));
        }

        @Test
        void givenExistingLabel_whenCreateLabel_thenThrowException() {
                // Arrange
                GenreDTO dto = GenreDTO.builder().name("Fiction").color("#FF0000").description("Fiction books").build();

                when(labelRepository.existsById("Fiction")).thenReturn(true);

                // Act
                assertThrows(ResourceAlreadyExistsException.class,
                                () -> labelService.createLabel(dto));

                // Assert
                verify(labelRepository, never()).save(any(Genre.class));
        }

        @Test
        void givenValidInput_whenCreateLabel_thenCreateSuccessfully() {
                // Arrange
                GenreDTO dto = GenreDTO.builder().name("Fiction").color("#FF0000").description("Fiction books").build();
                Genre entity = new Genre("Fiction", "#FF0000", "Fiction books");

                when(labelRepository.existsById("Fiction")).thenReturn(false);
                when(genreMapper.toEntity(dto)).thenReturn(entity);
                when(genreMapper.toDto(entity)).thenReturn(dto);

                // Act
                GenreDTO result = labelService.createLabel(dto);

                // Assert
                assertNotNull(result);
                assertEquals("Fiction", result.name());
                assertEquals("#FF0000", result.color());
                assertEquals("Fiction books", result.description());
                verify(labelRepository, times(1)).save(entity);
        }

        @Test
        void givenInvalidRequest_whenCreateLabel_thenThrowException() {
                // Arrange
                GenreDTO dto = GenreDTO.builder().build();
                Genre entity = new Genre(null, null, null);

                when(genreMapper.toEntity(dto)).thenReturn(entity);
                when(labelRepository.save(any(Genre.class)))
                                .thenThrow(new ConstraintViolationException("Validation failed", null));

                // Act
                assertThrows(ConstraintViolationException.class,
                                () -> labelService.createLabel(dto));

                // Assert
                verify(labelRepository, times(1)).save(any(Genre.class));
        }

        // ==================== getPage Tests ====================

        @Test
        void givenNoOptions_whenGetPage_thenReturnAllLabels() {
                // Arrange
                int page = 0;
                int pageSize = 10;
                LabelGetOptions options = new LabelGetOptions();

                Genre label = new Genre("Test", "#FF0000", "Test description");
                Page<Genre> labelPage = new PageImpl<>(List.of(label),
                                org.springframework.data.domain.PageRequest.of(page, pageSize), 1);

                when(labelRepository.findAll(any(Pageable.class))).thenReturn(labelPage);
                when(genreMapper.toDto(any(Genre.class))).thenReturn(
                                GenreDTO.builder().name("Test").color("#FF0000").description("Test desc").build());

                // Act
                PageDTO<GenreDTO> result = labelService.getPage(page, pageSize, options);

                // Assert
                assertNotNull(result);
                assertNotNull(result.data());
                assertNotNull(result.meta());
                verify(labelRepository, times(1)).findAll(any(Pageable.class));
        }

        @Test
        void givenIncludeBookCountTrue_whenGetPage_thenUseFindAllFiltered() {
                // Arrange
                int page = 0;
                int pageSize = 10;
                LabelGetOptions options = new LabelGetOptions();
                options.setIncludeBookCount(true);

                GenreWithCount projection = mock(GenreWithCount.class);
                Page<GenreWithCount> labelPage = new PageImpl<>(List.of(projection),
                                org.springframework.data.domain.PageRequest.of(page, pageSize), 1);

                when(labelRepository.findAllFiltered(any(), any(), any(Pageable.class))).thenReturn(labelPage);
                when(genreMapper.toDto(any(GenreProjection.class))).thenReturn(
                                GenreDTO.builder().name("Test").color("#FF0000").description("Test desc").build());

                // Act
                PageDTO<GenreDTO> result = labelService.getPage(page, pageSize, options);

                // Assert
                assertNotNull(result);
                verify(labelRepository, times(1)).findAllFiltered(any(), any(), any(Pageable.class));
                verify(labelRepository, never()).findAll(any(Pageable.class));
                verify(labelRepository, never()).searchByNamerOrDescriptionOrColor(any(), any(Pageable.class));
        }

        @Test
        void givenSearchQuery_whenGetPage_thenUseSearch() {
                // Arrange
                int page = 0;
                int pageSize = 10;
                LabelGetOptions options = new LabelGetOptions();
                options.setSearch("fiction");

                Genre label = new Genre("Fiction", "#FF0000", "Fiction books");
                Page<Genre> labelPage = new PageImpl<>(List.of(label),
                                org.springframework.data.domain.PageRequest.of(page, pageSize), 1);

                when(labelRepository.searchByNamerOrDescriptionOrColor(eq("fiction"), any(Pageable.class)))
                                .thenReturn(labelPage);
                when(genreMapper.toDto(any(Genre.class))).thenReturn(
                                GenreDTO.builder().name("Fiction").color("#FF0000").description("Fiction books")
                                                .build());

                // Act
                PageDTO<GenreDTO> result = labelService.getPage(page, pageSize, options);

                // Assert
                assertNotNull(result);
                verify(labelRepository, times(1)).searchByNamerOrDescriptionOrColor(eq("fiction"), any(Pageable.class));
                verify(labelRepository, never()).findAllFiltered(any(), any(), any(Pageable.class));
        }

        @Test
        void givenBookCountFilter_whenGetPage_thenUseFindAllFiltered() {
                // Arrange
                int page = 0;
                int pageSize = 10;
                LabelGetOptions options = new LabelGetOptions();
                options.setBookCount(1);

                GenreWithCount projection = mock(GenreWithCount.class);
                Page<GenreWithCount> labelPage = new PageImpl<>(List.of(projection),
                                org.springframework.data.domain.PageRequest.of(page, pageSize), 1);

                when(labelRepository.findAllFiltered(any(), eq(1), any(Pageable.class))).thenReturn(labelPage);
                when(genreMapper.toDto(any(GenreProjection.class))).thenReturn(
                                GenreDTO.builder().name("Test").color("#FF0000").description("Test desc").build());

                // Act
                PageDTO<GenreDTO> result = labelService.getPage(page, pageSize, options);

                // Assert
                assertNotNull(result);
                verify(labelRepository, times(1)).findAllFiltered(any(), eq(1), any(Pageable.class));
                verify(labelRepository, never()).searchByNamerOrDescriptionOrColor(any(), any(Pageable.class));
        }

        // ==================== getAllLabelStats Tests ====================

        @Test
        void givenLabelsExist_whenGetAllLabelStats_thenReturnCorrectStats() {
                // Arrange
                GenreWithCount label1 = mock(GenreWithCount.class);
                GenreWithCount label2 = mock(GenreWithCount.class);
                GenreWithCount label3 = mock(GenreWithCount.class);

                when(label1.getBookCount()).thenReturn(5);
                when(label2.getBookCount()).thenReturn(0);
                when(label3.getBookCount()).thenReturn(0);

                List<GenreWithCount> labels = List.of(label1, label2, label3);
                Page<GenreWithCount> labelPage = new PageImpl<>(labels);

                when(labelRepository.findAllIncludeBookCount(any())).thenReturn(labelPage);

                // Act
                GenreStatsDTO result = labelService.getAllGenreStats();

                // Assert
                assertNotNull(result);
                assertEquals(3, result.totalItems());
                assertEquals(2, result.unActiveLabels());
                verify(labelRepository, times(1)).findAllIncludeBookCount(any());
        }

        @Test
        void givenNoLabels_whenGetAllLabelStats_thenReturnZeroStats() {
                // Arrange
                Page<GenreWithCount> emptyPage = new PageImpl<>(Collections.emptyList());

                when(labelRepository.findAllIncludeBookCount(any())).thenReturn(emptyPage);

                // Act
                GenreStatsDTO result = labelService.getAllGenreStats();

                // Assert
                assertNotNull(result);
                assertEquals(0, result.totalItems());
                assertEquals(0, result.unActiveLabels());
                verify(labelRepository, times(1)).findAllIncludeBookCount(any());
        }

        @Test
        void givenAllLabelsHaveBooks_whenGetAllLabelStats_thenReturnCorrectStats() {
                // Arrange
                GenreWithCount label1 = mock(GenreWithCount.class);
                GenreWithCount label2 = mock(GenreWithCount.class);

                when(label1.getBookCount()).thenReturn(10);
                when(label2.getBookCount()).thenReturn(5);

                List<GenreWithCount> labels = List.of(label1, label2);
                Page<GenreWithCount> labelPage = new PageImpl<>(labels);

                when(labelRepository.findAllIncludeBookCount(any())).thenReturn(labelPage);

                // Act
                GenreStatsDTO result = labelService.getAllGenreStats();

                // Assert
                assertNotNull(result);
                assertEquals(2, result.totalItems());
                assertEquals(0, result.unActiveLabels());
                verify(labelRepository, times(1)).findAllIncludeBookCount(any());
        }

        // ==================== deleteById Tests ====================

        @Test
        void givenExistingLabelName_whenDeleteById_thenDeleteSuccessfully() {
                // Arrange
                String labelName = "Fiction";
                Genre label = new Genre(labelName, "#FF0000", "Fiction books");

                when(labelRepository.findById(labelName)).thenReturn(Optional.of(label));
                doNothing().when(labelRepository).deleteById(labelName);

                // Act
                labelService.deleteById(labelName);

                // Assert
                verify(labelRepository, times(1)).findById(labelName);
                verify(labelRepository, times(1)).deleteById(labelName);
        }

        @Test
        void givenNonExistingLabelName_whenDeleteById_thenThrowException() {
                // Arrange
                String labelName = "NonExistent";

                when(labelRepository.findById(labelName)).thenReturn(Optional.empty());

                // Act & Assert
                ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                                () -> labelService.deleteById(labelName));

                // Assert - message doesn't contain the label name, it's a static Dutch message
                assertNotNull(exception.getMessage());
                verify(labelRepository, times(1)).findById(labelName);
                verify(labelRepository, never()).deleteById(any());
        }

        @Test
        void givenLabelWithBooks_whenDeleteById_thenDeleteSuccessfully() {
                // Arrange
                String labelName = "Science";
                Genre label = new Genre(labelName, "#0000FF", "Science books");

                when(labelRepository.findById(labelName)).thenReturn(Optional.of(label));
                doNothing().when(labelRepository).deleteById(labelName);

                // Act
                labelService.deleteById(labelName);

                // Assert
                verify(labelRepository, times(1)).findById(labelName);
                verify(labelRepository, times(1)).deleteById(labelName);
        }

        // ==================== update Tests ====================

        @Test
        void givenExistingLabel_whenUpdate_thenUpdateSuccessfully() {
                // Arrange
                String labelName = "Fiction";
                GenreDTO inputDTO = GenreDTO.builder()
                                .name(labelName)
                                .color("#00FF00")
                                .description("Updated fiction description")
                                .build();
                Genre existingLabel = new Genre(labelName, "#FF0000", "Original description");
                Genre updatedLabel = new Genre(labelName, "#00FF00", "Updated fiction description");

                when(labelRepository.findById(labelName)).thenReturn(Optional.of(existingLabel));
                when(labelRepository.save(any(Genre.class))).thenReturn(updatedLabel);
                when(genreMapper.toDto(updatedLabel)).thenReturn(inputDTO);

                // Act
                GenreDTO result = labelService.update(labelName, inputDTO);

                // Assert
                assertNotNull(result);
                assertEquals("#00FF00", result.color());
                assertEquals("Updated fiction description", result.description());
                verify(labelRepository, times(1)).findById(labelName);
                verify(labelRepository, times(1)).save(any(Genre.class));
                verify(genreMapper, times(1)).toDto(updatedLabel);
        }

        @Test
        void givenNonExistingLabel_whenUpdate_thenThrowException() {
                // Arrange
                String labelName = "NonExistent";
                GenreDTO inputDTO = GenreDTO.builder()
                                .name(labelName)
                                .color("#00FF00")
                                .description("Some description")
                                .build();

                when(labelRepository.findById(labelName)).thenReturn(Optional.empty());

                // Act
                ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                                () -> labelService.update(labelName, inputDTO));

                // Assert
                assertNotNull(exception.getMessage());
                verify(labelRepository, times(1)).findById(labelName);
                verify(labelRepository, never()).save(any(Genre.class));
        }

        @Test
        void givenValidInputWithAllFields_whenUpdate_thenUpdateAllFields() {
                // Arrange
                String labelName = "Science";
                GenreDTO inputDTO = GenreDTO.builder()
                                .name(labelName)
                                .color("#0000FF")
                                .description("Science and technology books")
                                .build();
                Genre existingLabel = new Genre(labelName, "#FF0000", "Old description");
                Genre savedLabel = new Genre(labelName, "#0000FF", "Science and technology books");

                when(labelRepository.findById(labelName)).thenReturn(Optional.of(existingLabel));
                when(labelRepository.save(any(Genre.class))).thenReturn(savedLabel);
                when(genreMapper.toDto(savedLabel)).thenReturn(inputDTO);

                // Act
                GenreDTO result = labelService.update(labelName, inputDTO);

                // Assert
                assertNotNull(result);
                assertEquals("#0000FF", result.color());
                assertEquals("Science and technology books", result.description());
                verify(labelRepository, times(1)).findById(labelName);
                verify(labelRepository, times(1)).save(existingLabel);
        }

        @Test
        void givenUpdateOnlyColor_whenUpdate_thenUpdateColorOnly() {
                // Arrange
                String labelName = "Fiction";
                GenreDTO inputDTO = GenreDTO.builder()
                                .name(labelName)
                                .color("#FFFF00")
                                .description(null)
                                .build();
                Genre existingLabel = new Genre(labelName, "#FF0000", "Original description");
                Genre savedLabel = new Genre(labelName, "#FFFF00", "Original description");

                when(labelRepository.findById(labelName)).thenReturn(Optional.of(existingLabel));
                when(labelRepository.save(any(Genre.class))).thenReturn(savedLabel);
                when(genreMapper.toDto(savedLabel)).thenReturn(
                                GenreDTO.builder().name(labelName).color("#FFFF00").description("Original description")
                                                .build());

                // Act
                GenreDTO result = labelService.update(labelName, inputDTO);

                // Assert
                assertNotNull(result);
                verify(labelRepository, times(1)).findById(labelName);
                verify(labelRepository, times(1)).save(any(Genre.class));
        }
}
