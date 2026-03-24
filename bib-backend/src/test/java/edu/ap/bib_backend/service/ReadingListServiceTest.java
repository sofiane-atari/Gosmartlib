package edu.ap.bib_backend.service;

import edu.ap.bib_backend.dto.ReadingListDTO;
import edu.ap.bib_backend.jpa.entity.ReadingList;
import edu.ap.bib_backend.jpa.entity.ReadingListBook;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.repository.ReadingListBookRepository;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReadingListServiceTest {

    private static final String USER_ID = "user-1";
    private static final String CLASS_CODE = "2aICT";

    @Mock
    private ReadingListRepository readingListRepository;

    @Mock
    private ReadingListBookRepository readingListBookRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReadingListService readingListService;

    // createReadingList

    @Test
    void givenValidName_whenCreateReadingList_thenReturnCreatedList() {
        ReadingListDTO dto = new ReadingListDTO("My Reading List");

        ReadingList savedList = new ReadingList();
        savedList.setId(10L);
        savedList.setName("My Reading List");

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(makeUser()));
        when(readingListRepository.save(any(ReadingList.class))).thenReturn(savedList);

        ReadingList result = readingListService.createReadingList(USER_ID, dto);

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

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(makeUser()));
        when(readingListRepository.save(any(ReadingList.class))).thenReturn(savedList);

        ReadingList result = readingListService.createReadingList(USER_ID, dto);

        assertEquals("Mijn Lijst", result.getName());
        verify(readingListRepository).save(any(ReadingList.class));
    }

    @Test
    void givenEmptyName_whenCreateReadingList_thenThrowException() {
        ReadingListDTO dto = new ReadingListDTO("");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> readingListService.createReadingList(USER_ID, dto));

        assertEquals("name mag niet null of leeg zijn", exception.getMessage());
        verify(readingListRepository, never()).save(any());
    }

    @Test
    void givenNullName_whenCreateReadingList_thenThrowException() {
        ReadingListDTO dto = new ReadingListDTO(null);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> readingListService.createReadingList(USER_ID, dto));

        assertEquals("name mag niet null of leeg zijn", exception.getMessage());
        verify(readingListRepository, never()).save(any());
    }

    // getAllLists

    @BeforeEach
    void setUp() {
        lenient().when(userRepository.findById(USER_ID)).thenReturn(Optional.of(makeUser()));
    }

    @Test
    void givenExistingLists_whenGetAllLists_thenReturnAllLists() {
        ReadingList list = new ReadingList();
        list.setId(10L);
        list.setName("Existing List");

        ReadingList classList = new ReadingList();
        classList.setId(20L);
        classList.setName("2 aICT");
        classList.setIsClassList(true);

        when(readingListRepository.findByClassCodeAndIsClassListTrue(CLASS_CODE)).thenReturn(Optional.of(classList));
        when(readingListRepository.findVisibleForUser(USER_ID, CLASS_CODE)).thenReturn(Collections.singletonList(list));

        List<ReadingList> result = readingListService.getAllLists(USER_ID);

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals("Existing List", result.get(0).getName());
        verify(readingListRepository).findVisibleForUser(USER_ID, CLASS_CODE);
    }

    @Test
    void givenNoLists_whenGetAllLists_thenReturnEmptyList() {
        when(readingListRepository.findByClassCodeAndIsClassListTrue(CLASS_CODE)).thenReturn(Optional.of(new ReadingList()));
        when(readingListRepository.findVisibleForUser(USER_ID, CLASS_CODE)).thenReturn(Collections.emptyList());

        List<ReadingList> result = readingListService.getAllLists(USER_ID);

        assertTrue(result.isEmpty());
        verify(readingListRepository).findVisibleForUser(USER_ID, CLASS_CODE);
    }

    @Test
    void givenMissingClassList_whenGetAllLists_thenCreateClassList() {
        when(readingListRepository.findByClassCodeAndIsClassListTrue(CLASS_CODE)).thenReturn(Optional.empty());
        when(readingListRepository.findVisibleForUser(USER_ID, CLASS_CODE)).thenReturn(Collections.emptyList());

        readingListService.getAllLists(USER_ID);

        verify(readingListRepository).save(argThat(list ->
                Boolean.TRUE.equals(list.getIsClassList())
                        && CLASS_CODE.equals(list.getClassCode())
                        && "2 aICT".equals(list.getName())));
    }

    // deleteReadingList

    @Test
    void givenExistingId_whenDeleteReadingList_thenDeleteSuccessfully() {
        Long id = 10L;
        ReadingList list = new ReadingList();
        list.setId(id);
        list.setName("Gewone lijst");
        when(readingListRepository.findVisibleById(id, USER_ID, CLASS_CODE)).thenReturn(Optional.of(list));

        readingListService.deleteReadingList(USER_ID, id);

        verify(readingListRepository).deleteById(id);
    }

    @Test
    void givenFavoritesList_whenDeleteReadingList_thenThrowException() {
        Long id = 11L;
        ReadingList list = new ReadingList();
        list.setId(id);
        list.setName("Favorieten");
        when(readingListRepository.findVisibleById(id, USER_ID, CLASS_CODE)).thenReturn(Optional.of(list));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> readingListService.deleteReadingList(USER_ID, id));

        assertEquals("Deze leeslijst kan niet verwijderd worden", exception.getMessage());
        verify(readingListRepository, never()).deleteById(any());
    }

    @Test
    void givenClassList_whenDeleteReadingList_thenThrowException() {
        Long id = 12L;
        ReadingList list = new ReadingList();
        list.setId(id);
        list.setName("3 economie");
        list.setIsClassList(true);
        when(readingListRepository.findVisibleById(id, USER_ID, CLASS_CODE)).thenReturn(Optional.of(list));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> readingListService.deleteReadingList(USER_ID, id));

        assertEquals("Deze leeslijst kan niet verwijderd worden", exception.getMessage());
        verify(readingListRepository, never()).deleteById(any());
    }

    @Test
    void givenNonExistingId_whenDeleteReadingList_thenThrowException() {
        Long id = 99L;
        when(readingListRepository.findVisibleById(id, USER_ID, CLASS_CODE)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> readingListService.deleteReadingList(USER_ID, id));

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

        when(readingListRepository.findVisibleById(id, USER_ID, CLASS_CODE)).thenReturn(Optional.of(list));

        ReadingList result = readingListService.getListById(USER_ID, id);

        assertNotNull(result);
        assertEquals("Test lijst", result.getName());
        verify(readingListRepository).findVisibleById(id, USER_ID, CLASS_CODE);
    }

    @Test
    void givenNonExistingId_whenGetListById_thenThrowException() {
        Long id = 99L;
        when(readingListRepository.findVisibleById(id, USER_ID, CLASS_CODE)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> readingListService.getListById(USER_ID, id));

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

        when(readingListRepository.findVisibleById(listId, USER_ID, CLASS_CODE)).thenReturn(Optional.of(list));
        when(readingListBookRepository.existsByReadingList_IdAndBookId(listId, bookId)).thenReturn(false);
        when(readingListBookRepository.save(any(ReadingListBook.class))).thenReturn(savedBook);

        ReadingListBook result = readingListService.addBookToList(USER_ID, listId, bookId);

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

        when(readingListRepository.findVisibleById(listId, USER_ID, CLASS_CODE)).thenReturn(Optional.of(list));
        when(readingListBookRepository.existsByReadingList_IdAndBookId(listId, bookId)).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> readingListService.addBookToList(USER_ID, listId, bookId));

        assertEquals("Boek zit al in deze leeslijst", exception.getMessage());
        verify(readingListBookRepository, never()).save(any());
    }

    @Test
    void givenNullBookId_whenAddBookToList_thenThrowException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> readingListService.addBookToList(USER_ID, 1L, null));

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

        when(readingListRepository.findByNameIgnoreCaseAndUser_Id("Favorieten", USER_ID)).thenReturn(Optional.of(list));
        when(readingListRepository.findVisibleById(99L, USER_ID, CLASS_CODE)).thenReturn(Optional.of(list));
        when(readingListBookRepository.save(any(ReadingListBook.class))).thenReturn(savedBook);

        ReadingListBook result = readingListService.addBookToFavorites(USER_ID, bookId);

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

        when(readingListRepository.findByNameIgnoreCaseAndUser_Id("Favorieten", USER_ID)).thenReturn(Optional.empty());
        when(readingListRepository.save(any(ReadingList.class))).thenReturn(newList);
        when(readingListRepository.findVisibleById(100L, USER_ID, CLASS_CODE)).thenReturn(Optional.of(newList));
        when(readingListBookRepository.save(any(ReadingListBook.class))).thenReturn(savedBook);

        ReadingListBook result = readingListService.addBookToFavorites(USER_ID, bookId);

        assertNotNull(result);
        assertEquals(bookId, result.getBookId());
        verify(readingListRepository).save(any(ReadingList.class));
        verify(readingListBookRepository).save(any());
    }

    // removeBookFromList

    @Test
    void givenBookInList_whenRemoveBookFromList_thenDeleteLink() {
        Long listId = 10L;
        Long bookId = 100L;

        ReadingList list = new ReadingList();
        list.setId(listId);

        when(readingListRepository.findVisibleById(listId, USER_ID, CLASS_CODE)).thenReturn(Optional.of(list));
        when(readingListBookRepository.deleteAllByReadingListIdAndBookId(listId, bookId)).thenReturn(1);

        readingListService.removeBookFromList(USER_ID, listId, bookId);

        verify(readingListBookRepository).deleteAllByReadingListIdAndBookId(listId, bookId);
    }

    @Test
    void givenBookNotInList_whenRemoveBookFromList_thenThrowException() {
        Long listId = 10L;
        Long bookId = 999L;

        ReadingList list = new ReadingList();
        list.setId(listId);

        when(readingListRepository.findVisibleById(listId, USER_ID, CLASS_CODE)).thenReturn(Optional.of(list));
        when(readingListBookRepository.deleteAllByReadingListIdAndBookId(listId, bookId)).thenReturn(0);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> readingListService.removeBookFromList(USER_ID, listId, bookId));

        assertEquals("Boek met id 999 zit niet in deze leeslijst", exception.getMessage());
        verify(readingListBookRepository, never()).delete(any());
        verify(readingListBookRepository, never()).deleteAll(anyList());
    }

    @Test
    void givenClassList_whenRemoveBookFromList_thenThrowException() {
        Long listId = 10L;
        Long bookId = 100L;

        ReadingList classList = new ReadingList();
        classList.setId(listId);
        classList.setIsClassList(true);

        when(readingListRepository.findVisibleById(listId, USER_ID, CLASS_CODE)).thenReturn(Optional.of(classList));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> readingListService.removeBookFromList(USER_ID, listId, bookId));

        assertEquals("Boeken uit de klassikale leeslijst kunnen niet verwijderd worden", exception.getMessage());
        verify(readingListBookRepository, never()).deleteAllByReadingListIdAndBookId(anyLong(), anyLong());
    }

    private User makeUser() {
        User user = new User();
        user.setId(USER_ID);
        user.setClassCode(CLASS_CODE);
        return user;
    }
}
