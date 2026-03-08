package edu.ap.bib_backend.service;

import edu.ap.bib_backend.dto.ReadingListDTO;
import edu.ap.bib_backend.jpa.entity.ReadingList;
import edu.ap.bib_backend.jpa.entity.ReadingListBook;
import edu.ap.bib_backend.jpa.repository.ReadingListBookRepository;
import edu.ap.bib_backend.jpa.repository.ReadingListRepository;
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
    private ReadingListBookRepository readingListBookRepository;

    @InjectMocks
    private ReadingListService readingListService;

    // createReadingList

    @Test
    void givenValidName_whenCreateReadingList_thenReturnCreatedList() {
        ReadingListDTO dto = new ReadingListDTO("My Reading List");

        ReadingList savedList = new ReadingList();
        savedList.setId(10L);
        savedList.setName("My Reading List");

        when(readingListRepository.save(any(ReadingList.class))).thenReturn(savedList);

        ReadingList result = readingListService.createReadingList(dto);

        assertNotNull(result);
        assertEquals("My Reading List", result.getName());
        verify(readingListRepository).save(any(ReadingList.class));
    }

    @Test
    void givenNameWithWhitespace_whenCreateReadingList_thenTrimAndSave() {
        ReadingListDTO dto = new ReadingListDTO("  Mijn Lijst  ");

        ReadingList savedList = new ReadingList();
        savedList.setId(11L);
        savedList.setName("Mijn Lijst");

        when(readingListRepository.save(any(ReadingList.class))).thenReturn(savedList);

        ReadingList result = readingListService.createReadingList(dto);

        assertEquals("Mijn Lijst", result.getName());
        verify(readingListRepository).save(any(ReadingList.class));
    }

    @Test
    void givenEmptyName_whenCreateReadingList_thenThrowException() {
        ReadingListDTO dto = new ReadingListDTO("");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> readingListService.createReadingList(dto));

        assertEquals("name mag niet null of leeg zijn", exception.getMessage());
        verify(readingListRepository, never()).save(any());
    }

    @Test
    void givenNullName_whenCreateReadingList_thenThrowException() {
        ReadingListDTO dto = new ReadingListDTO(null);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> readingListService.createReadingList(dto));

        assertEquals("name mag niet null of leeg zijn", exception.getMessage());
        verify(readingListRepository, never()).save(any());
    }

    // getAllLists

    @Test
    void givenExistingLists_whenGetAllLists_thenReturnAllLists() {
        ReadingList list = new ReadingList();
        list.setId(10L);
        list.setName("Existing List");

        when(readingListRepository.findAll()).thenReturn(Collections.singletonList(list));

        List<ReadingList> result = readingListService.getAllLists();

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals("Existing List", result.get(0).getName());
        verify(readingListRepository).findAll();
    }

    @Test
    void givenNoLists_whenGetAllLists_thenReturnEmptyList() {
        when(readingListRepository.findAll()).thenReturn(Collections.emptyList());

        List<ReadingList> result = readingListService.getAllLists();

        assertTrue(result.isEmpty());
        verify(readingListRepository).findAll();
    }

    // deleteReadingList

    @Test
    void givenExistingId_whenDeleteReadingList_thenDeleteSuccessfully() {
        Long id = 10L;
        when(readingListRepository.existsById(id)).thenReturn(true);

        readingListService.deleteReadingList(id);

        verify(readingListRepository).deleteById(id);
    }

    @Test
    void givenNonExistingId_whenDeleteReadingList_thenThrowException() {
        Long id = 99L;
        when(readingListRepository.existsById(id)).thenReturn(false);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> readingListService.deleteReadingList(id));

        assertEquals("Leeslijst met id 99 bestaat niet", exception.getMessage());
        verify(readingListRepository, never()).deleteById(any());
    }

    // getListById

    @Test
    void givenExistingId_whenGetListById_thenReturnList() {
        Long id = 10L;
        ReadingList list = new ReadingList();
        list.setId(id);
        list.setName("Test lijst");

        when(readingListRepository.findById(id)).thenReturn(java.util.Optional.of(list));

        ReadingList result = readingListService.getListById(id);

        assertNotNull(result);
        assertEquals("Test lijst", result.getName());
        verify(readingListRepository).findById(id);
    }

    @Test
    void givenNonExistingId_whenGetListById_thenThrowException() {
        Long id = 99L;
        when(readingListRepository.findById(id)).thenReturn(java.util.Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> readingListService.getListById(id));

        assertEquals("Leeslijst met id 99 bestaat niet", exception.getMessage());
    }

    // addBookToList

    @Test
    void givenValidBookId_whenAddBookToList_thenSaveAndReturn() {
        Long listId = 1L;
        Long bookId = 100L;
        ReadingList list = new ReadingList();
        list.setId(listId);

        ReadingListBook savedBook = new ReadingListBook();
        savedBook.setId(10L);
        savedBook.setReadingList(list);
        savedBook.setBookId(bookId);

        when(readingListRepository.findById(listId)).thenReturn(java.util.Optional.of(list));
        when(readingListBookRepository.save(any(ReadingListBook.class))).thenReturn(savedBook);

        ReadingListBook result = readingListService.addBookToList(listId, bookId);

        assertNotNull(result);
        assertEquals(bookId, result.getBookId());
        assertEquals(listId, result.getReadingList().getId());
        verify(readingListBookRepository).save(any(ReadingListBook.class));
    }

    @Test
    void givenBookAlreadyInList_whenAddBookToList_thenThrowException() {
        Long listId = 1L;
        Long bookId = 100L;
        ReadingList list = new ReadingList();
        list.setId(listId);

        ReadingListBook existingBook = new ReadingListBook();
        existingBook.setBookId(bookId);
        list.setBooks(List.of(existingBook));

        when(readingListRepository.findById(listId)).thenReturn(java.util.Optional.of(list));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> readingListService.addBookToList(listId, bookId));

        assertEquals("Boek zit al in deze leeslijst", exception.getMessage());
        verify(readingListBookRepository, never()).save(any());
    }

    @Test
    void givenNullBookId_whenAddBookToList_thenThrowException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> readingListService.addBookToList(1L, null));

        assertEquals("Boek ID mag niet null zijn", exception.getMessage());
    }

    // addBookToFavorites

    @Test
    void givenFavoritesListExists_whenAddBookToFavorites_thenAddToList() {
        Long bookId = 100L;
        ReadingList list = new ReadingList();
        list.setId(99L);
        list.setName("Favorieten");

        ReadingListBook savedBook = new ReadingListBook();
        savedBook.setBookId(bookId);
        savedBook.setReadingList(list);

        when(readingListRepository.findByNameIgnoreCase("Favorieten")).thenReturn(java.util.Optional.of(list));
        when(readingListRepository.findById(99L)).thenReturn(java.util.Optional.of(list));
        when(readingListBookRepository.save(any(ReadingListBook.class))).thenReturn(savedBook);

        ReadingListBook result = readingListService.addBookToFavorites(bookId);

        assertNotNull(result);
        assertEquals(bookId, result.getBookId());
        verify(readingListRepository, never()).save(any());
        verify(readingListBookRepository).save(any());
    }

    @Test
    void givenFavoritesListDoesNotExist_whenAddBookToFavorites_thenCreateListAndAdd() {
        Long bookId = 100L;
        ReadingList newList = new ReadingList();
        newList.setId(100L);
        newList.setName("Favorieten");

        ReadingListBook savedBook = new ReadingListBook();
        savedBook.setBookId(bookId);
        savedBook.setReadingList(newList);

        when(readingListRepository.findByNameIgnoreCase("Favorieten")).thenReturn(java.util.Optional.empty());
        when(readingListRepository.save(any(ReadingList.class))).thenReturn(newList);
        when(readingListRepository.findById(100L)).thenReturn(java.util.Optional.of(newList));
        when(readingListBookRepository.save(any(ReadingListBook.class))).thenReturn(savedBook);

        ReadingListBook result = readingListService.addBookToFavorites(bookId);

        assertNotNull(result);
        assertEquals(bookId, result.getBookId());
        verify(readingListRepository).save(any(ReadingList.class));
        verify(readingListBookRepository).save(any());
    }
}
