package edu.ap.bib_backend.service;

import edu.ap.bib_backend.dto.ReadingListDTO;
import edu.ap.bib_backend.jpa.entity.ReadingList;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.repository.ReadingListRepository;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReadingListServiceTest {

    @Mock
    private ReadingListRepository readingListRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReadingListService readingListService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setDisplayName("Test User");
    }

    @Test
    void createReadingList_ValidData_ShouldReturnCreatedList() {
        // Arrange
        ReadingListDTO dto = new ReadingListDTO(1L, "My Reading List");
        when(userRepository.getReferenceById(1L)).thenReturn(mockUser);

        ReadingList savedList = new ReadingList();
        savedList.setId(10L);
        savedList.setName("My Reading List");
        savedList.setUser(mockUser);

        when(readingListRepository.save(any(ReadingList.class))).thenReturn(savedList);

        // Act
        ReadingList result = readingListService.createReadingList(dto);

        // Assert
        assertNotNull(result);
        assertEquals("My Reading List", result.getName());
        assertEquals(mockUser, result.getUser());
        verify(readingListRepository).save(any(ReadingList.class));
    }

    @Test
    void createReadingList_EmptyName_ShouldThrowException() {
        // Arrange
        ReadingListDTO dto = new ReadingListDTO(1L, "");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> readingListService.createReadingList(dto));

        assertEquals("name mag niet null of leeg zijn", exception.getMessage());
        verify(readingListRepository, never()).save(any());
    }

    @Test
    void getListByUser_ShouldReturnList() {
        // Arrange
        Long userId = 1L;
        ReadingList list = new ReadingList();
        list.setId(10L);
        list.setName("Existing List");

        when(readingListRepository.findByUserId(userId)).thenReturn(Collections.singletonList(list));

        // Act
        List<ReadingList> result = readingListService.getListByUser(userId);

        // Assert
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals("Existing List", result.get(0).getName());
        verify(readingListRepository).findByUserId(userId);
    }
}
