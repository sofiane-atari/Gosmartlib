package edu.ap.bib_backend;

import edu.ap.bib_backend.dto.LabelDTO;
import edu.ap.bib_backend.jpa.entity.Label;
import edu.ap.bib_backend.jpa.repository.LabelRepository;
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

    @InjectMocks
    private LabelService labelService;

    @Test
    void createLabel_ShouldThrowException_WhenLabelNameIsNull() {
        // Arrange
        LabelDTO dto = new LabelDTO(null, "Some description", "#FF0000");
        when(labelRepository.save(any(Label.class)))
                .thenThrow(new IllegalArgumentException("Label name must not be null"));

        // Act
        Exception exception = assertThrows(IllegalArgumentException.class, () -> labelService.createLabel(dto));

        // Assert
        assertEquals("Label name must not be null", exception.getMessage());
        verify(labelRepository, times(1)).save(any(Label.class));
    }

    @Test
    void createLabel_ShouldThrowException_WhenLabelNameIsEmpty() {
        // Arrange
        LabelDTO dto = new LabelDTO("", "Some description", "#FF0000");
        when(labelRepository.save(any(Label.class)))
                .thenThrow(new IllegalArgumentException("Label name must not be empty"));

        // Act
        Exception exception = assertThrows(IllegalArgumentException.class, () -> labelService.createLabel(dto));

        // Assert
        assertEquals("Label name must not be empty", exception.getMessage());
        verify(labelRepository, times(1)).save(any(Label.class));
    }

    @Test
    void createLabel_ShouldThrowException_WhenLabelAlreadyExists() {
        // Arrange
        LabelDTO dto = new LabelDTO("Fiction", "Fiction books", "#FF0000");
        when(labelRepository.save(any(Label.class)))
                .thenThrow(new IllegalStateException("Label already exists"));

        // Act
        Exception exception = assertThrows(IllegalStateException.class, () -> labelService.createLabel(dto));

        // Assert
        assertEquals("Label already exists", exception.getMessage());
        verify(labelRepository, times(1)).save(any(Label.class));
    }

    @Test
    void createLabel_ShouldCreateLabelSuccessfully_WhenInputIsValid() {
        // Arrange
        LabelDTO dto = new LabelDTO("Fiction", "Fiction books", "#FF0000");
        Label savedLabel = new Label("Fiction", "Fiction books", "#FF0000");
        when(labelRepository.save(any(Label.class))).thenReturn(savedLabel);

        // Act
        LabelDTO result = labelService.createLabel(dto);

        // Assert
        assertNotNull(result);
        assertEquals("Fiction", result.name());
        assertEquals("Fiction books", result.description());
        assertEquals("#FF0000", result.color());
        verify(labelRepository, times(1)).save(any(Label.class));
    }

    @Test
    void createLabel_ShouldNotCreateLabel_WhenRequestIsInvalid() {
        // Arrange
        LabelDTO dto = new LabelDTO(null, null, null);
        when(labelRepository.save(any(Label.class)))
                .thenThrow(new ConstraintViolationException("Validation failed", null));

        // Act
        Exception exception = assertThrows(ConstraintViolationException.class, () -> labelService.createLabel(dto));

        // Assert
        assertEquals("Validation failed", exception.getMessage());
        verify(labelRepository, never()).save(argThat(label -> label.getName() != null));
    }
}