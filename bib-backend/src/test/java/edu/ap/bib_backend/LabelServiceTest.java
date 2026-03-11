package edu.ap.bib_backend;

import edu.ap.bib_backend.dto.LabelDTO;
import edu.ap.bib_backend.dto.LabelStatsDTO;
import edu.ap.bib_backend.dto.PageDTO;
import edu.ap.bib_backend.exception.ResourceAlreadyExistsException;
import edu.ap.bib_backend.jpa.entity.Label;
import edu.ap.bib_backend.jpa.repository.LabelRepository;
import edu.ap.bib_backend.mapper.LabelMapper;
import edu.ap.bib_backend.options.LabelGetOptions;
import edu.ap.bib_backend.projection.LabelProjection;
import edu.ap.bib_backend.projection.LabelWithCount;
import edu.ap.bib_backend.service.LabelService;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LabelServiceTest {

        @Mock
        private LabelRepository labelRepository;

        @Mock
        private LabelMapper labelMapper;

        @InjectMocks
        private LabelService labelService;

        // ==================== createLabel Tests ====================

        @Test
        void givenNullName_whenCreateLabel_thenThrowException() {
                // Arrange
                LabelDTO dto = LabelDTO.builder().name(null).color("#FF0000").description("Some description").build();
                Label entity = new Label(null, "#FF0000", "Some description");

                when(labelMapper.toEntity(dto)).thenReturn(entity);
                when(labelRepository.save(any(Label.class)))
                                .thenThrow(new IllegalArgumentException("Label name must not be null"));

                // Act
                Exception exception = assertThrows(IllegalArgumentException.class,
                                () -> labelService.createLabel(dto));

                // Assert
                assertEquals("Label name must not be null", exception.getMessage());
                verify(labelRepository, times(1)).save(any(Label.class));
        }

        @Test
        void givenEmptyName_whenCreateLabel_thenThrowException() {
                // Arrange
                LabelDTO dto = LabelDTO.builder().name("").color("#FF0000").description("Some description").build();
                Label entity = new Label("", "#FF0000", "Some description");

                when(labelMapper.toEntity(dto)).thenReturn(entity);
                when(labelRepository.save(any(Label.class)))
                                .thenThrow(new IllegalArgumentException("Label name must not be empty"));

                // Act
                Exception exception = assertThrows(IllegalArgumentException.class,
                                () -> labelService.createLabel(dto));

                // Assert
                assertEquals("Label name must not be empty", exception.getMessage());
                verify(labelRepository, times(1)).save(any(Label.class));
        }

        @Test
        void givenExistingLabel_whenCreateLabel_thenThrowException() {
                // Arrange
                LabelDTO dto = LabelDTO.builder().name("Fiction").color("#FF0000").description("Fiction books").build();

                when(labelRepository.existsById("Fiction")).thenReturn(true);

                // Act
                assertThrows(ResourceAlreadyExistsException.class,
                                () -> labelService.createLabel(dto));

                // Assert
                verify(labelRepository, never()).save(any(Label.class));
        }

        @Test
        void givenValidInput_whenCreateLabel_thenCreateSuccessfully() {
                // Arrange
                LabelDTO dto = LabelDTO.builder().name("Fiction").color("#FF0000").description("Fiction books").build();
                Label entity = new Label("Fiction", "#FF0000", "Fiction books");

                when(labelRepository.existsById("Fiction")).thenReturn(false);
                when(labelMapper.toEntity(dto)).thenReturn(entity);
                when(labelMapper.toDto(entity)).thenReturn(dto);

                // Act
                LabelDTO result = labelService.createLabel(dto);

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
                LabelDTO dto = LabelDTO.builder().build();
                Label entity = new Label(null, null, null);

                when(labelMapper.toEntity(dto)).thenReturn(entity);
                when(labelRepository.save(any(Label.class)))
                                .thenThrow(new ConstraintViolationException("Validation failed", null));

                // Act
                assertThrows(ConstraintViolationException.class,
                                () -> labelService.createLabel(dto));

                // Assert
                verify(labelRepository, times(1)).save(any(Label.class));
        }

        // ==================== getPage Tests ====================

        @Test
        void givenNoOptions_whenGetPage_thenReturnAllLabels() {
                // Arrange
                int page = 0;
                int pageSize = 10;
                LabelGetOptions options = new LabelGetOptions();

                Label label = new Label("Test", "#FF0000", "Test description");
                Page<Label> labelPage = new PageImpl<>(List.of(label),
                                org.springframework.data.domain.PageRequest.of(page, pageSize), 1);

                when(labelRepository.findAll(any(Pageable.class))).thenReturn(labelPage);
                when(labelMapper.toDto(any(Label.class))).thenReturn(
                                LabelDTO.builder().name("Test").color("#FF0000").description("Test desc").build());

                // Act
                PageDTO<LabelDTO> result = labelService.getPage(page, pageSize, options);

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

                LabelWithCount projection = mock(LabelWithCount.class);
                Page<LabelWithCount> labelPage = new PageImpl<>(List.of(projection),
                                org.springframework.data.domain.PageRequest.of(page, pageSize), 1);

                when(labelRepository.findAllFiltered(any(), any(), any(Pageable.class))).thenReturn(labelPage);
                when(labelMapper.toDto(any(LabelProjection.class))).thenReturn(
                                LabelDTO.builder().name("Test").color("#FF0000").description("Test desc").build());

                // Act
                PageDTO<LabelDTO> result = labelService.getPage(page, pageSize, options);

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

                Label label = new Label("Fiction", "#FF0000", "Fiction books");
                Page<Label> labelPage = new PageImpl<>(List.of(label),
                                org.springframework.data.domain.PageRequest.of(page, pageSize), 1);

                when(labelRepository.searchByNamerOrDescriptionOrColor(eq("fiction"), any(Pageable.class)))
                                .thenReturn(labelPage);
                when(labelMapper.toDto(any(Label.class))).thenReturn(
                                LabelDTO.builder().name("Fiction").color("#FF0000").description("Fiction books")
                                                .build());

                // Act
                PageDTO<LabelDTO> result = labelService.getPage(page, pageSize, options);

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

                LabelWithCount projection = mock(LabelWithCount.class);
                Page<LabelWithCount> labelPage = new PageImpl<>(List.of(projection),
                                org.springframework.data.domain.PageRequest.of(page, pageSize), 1);

                when(labelRepository.findAllFiltered(any(), eq(1), any(Pageable.class))).thenReturn(labelPage);
                when(labelMapper.toDto(any(LabelProjection.class))).thenReturn(
                                LabelDTO.builder().name("Test").color("#FF0000").description("Test desc").build());

                // Act
                PageDTO<LabelDTO> result = labelService.getPage(page, pageSize, options);

                // Assert
                assertNotNull(result);
                verify(labelRepository, times(1)).findAllFiltered(any(), eq(1), any(Pageable.class));
                verify(labelRepository, never()).searchByNamerOrDescriptionOrColor(any(), any(Pageable.class));
        }

        // ==================== getAllLabelStats Tests ====================

        @Test
        void givenLabelsExist_whenGetAllLabelStats_thenReturnCorrectStats() {
                // Arrange
                LabelWithCount label1 = mock(LabelWithCount.class);
                LabelWithCount label2 = mock(LabelWithCount.class);
                LabelWithCount label3 = mock(LabelWithCount.class);

                when(label1.getBookCount()).thenReturn(5);
                when(label2.getBookCount()).thenReturn(0);
                when(label3.getBookCount()).thenReturn(0);

                List<LabelWithCount> labels = List.of(label1, label2, label3);
                Page<LabelWithCount> labelPage = new PageImpl<>(labels);

                when(labelRepository.findAllIncludeBookCount(any())).thenReturn(labelPage);

                // Act
                LabelStatsDTO result = labelService.getAllLabelStats();

                // Assert
                assertNotNull(result);
                assertEquals(3, result.totalItems());
                assertEquals(2, result.unActiveLabels());
                verify(labelRepository, times(1)).findAllIncludeBookCount(any());
        }

        @Test
        void givenNoLabels_whenGetAllLabelStats_thenReturnZeroStats() {
                // Arrange
                Page<LabelWithCount> emptyPage = new PageImpl<>(Collections.emptyList());

                when(labelRepository.findAllIncludeBookCount(any())).thenReturn(emptyPage);

                // Act
                LabelStatsDTO result = labelService.getAllLabelStats();

                // Assert
                assertNotNull(result);
                assertEquals(0, result.totalItems());
                assertEquals(0, result.unActiveLabels());
                verify(labelRepository, times(1)).findAllIncludeBookCount(any());
        }

        @Test
        void givenAllLabelsHaveBooks_whenGetAllLabelStats_thenReturnCorrectStats() {
                // Arrange
                LabelWithCount label1 = mock(LabelWithCount.class);
                LabelWithCount label2 = mock(LabelWithCount.class);

                when(label1.getBookCount()).thenReturn(10);
                when(label2.getBookCount()).thenReturn(5);

                List<LabelWithCount> labels = List.of(label1, label2);
                Page<LabelWithCount> labelPage = new PageImpl<>(labels);

                when(labelRepository.findAllIncludeBookCount(any())).thenReturn(labelPage);

                // Act
                LabelStatsDTO result = labelService.getAllLabelStats();

                // Assert
                assertNotNull(result);
                assertEquals(2, result.totalItems());
                assertEquals(0, result.unActiveLabels());
                verify(labelRepository, times(1)).findAllIncludeBookCount(any());
        }
}