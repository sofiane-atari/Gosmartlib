package edu.ap.bib_backend;

import edu.ap.bib_backend.dto.LabelDTO;
import edu.ap.bib_backend.exception.ResourceAlreadyExistsException;
import edu.ap.bib_backend.jpa.entity.Label;
import edu.ap.bib_backend.jpa.repository.LabelRepository;
import edu.ap.bib_backend.mapper.LabelMapper;
import edu.ap.bib_backend.service.LabelService;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LabelServiceTest {

    @Mock
    private LabelRepository labelRepository;

    @Mock
    private LabelMapper labelMapper;  // ← ADD THIS

    @InjectMocks
    private LabelService labelService;

    @Test
    void createLabel_ShouldThrowException_WhenLabelNameIsNull() {
        LabelDTO dto = new LabelDTO(null, "#FF0000", "Some description");
        Label entity = new Label(null, "#FF0000", "Some description");

        when(labelMapper.toEntity(dto)).thenReturn(entity);
        when(labelRepository.save(any(Label.class)))
                .thenThrow(new IllegalArgumentException("Label name must not be null"));

        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> labelService.createLabel(dto));

        assertEquals("Label name must not be null", exception.getMessage());
        verify(labelRepository, times(1)).save(any(Label.class));
    }

    @Test
    void createLabel_ShouldThrowException_WhenLabelNameIsEmpty() {
        LabelDTO dto = new LabelDTO("", "#FF0000", "Some description");
        Label entity = new Label("", "#FF0000", "Some description");

        when(labelMapper.toEntity(dto)).thenReturn(entity);
        when(labelRepository.save(any(Label.class)))
                .thenThrow(new IllegalArgumentException("Label name must not be empty"));

        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> labelService.createLabel(dto));

        assertEquals("Label name must not be empty", exception.getMessage());
        verify(labelRepository, times(1)).save(any(Label.class));
    }

    @Test
    void createLabel_ShouldThrowException_WhenLabelAlreadyExists() {
        LabelDTO dto = new LabelDTO("Fiction", "#FF0000", "Fiction books");

        // existsById returns true → service throws ResourceAlreadyExistsException before mapper
        when(labelRepository.existsById("Fiction")).thenReturn(true);

        assertThrows(ResourceAlreadyExistsException.class,
                () -> labelService.createLabel(dto));

        verify(labelRepository, never()).save(any(Label.class));
    }

    @Test
    void createLabel_ShouldCreateLabelSuccessfully_WhenInputIsValid() {
        LabelDTO dto = new LabelDTO("Fiction", "#FF0000", "Fiction books");
        Label entity = new Label("Fiction", "#FF0000", "Fiction books");

        when(labelRepository.existsById("Fiction")).thenReturn(false);
        when(labelMapper.toEntity(dto)).thenReturn(entity);
        when(labelMapper.toDto(entity)).thenReturn(dto);

        LabelDTO result = labelService.createLabel(dto);

        assertNotNull(result);
        assertEquals("Fiction", result.name());
        assertEquals("#FF0000", result.color());
        assertEquals("Fiction books", result.description());
        verify(labelRepository, times(1)).save(entity);
    }

    @Test
    void createLabel_ShouldNotCreateLabel_WhenRequestIsInvalid() {
        LabelDTO dto = new LabelDTO(null, null, null);
        Label entity = new Label(null, null, null);

        when(labelMapper.toEntity(dto)).thenReturn(entity);
        when(labelRepository.save(any(Label.class)))
                .thenThrow(new ConstraintViolationException("Validation failed", null));

        assertThrows(ConstraintViolationException.class,
                () -> labelService.createLabel(dto));

        verify(labelRepository, times(1)).save(any(Label.class));
    }
}