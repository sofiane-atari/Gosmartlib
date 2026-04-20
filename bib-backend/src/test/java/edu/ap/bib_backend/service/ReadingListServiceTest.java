package edu.ap.bib_backend.service;

import edu.ap.bib_backend.dto.ReadingListDTO;
import edu.ap.bib_backend.jpa.entity.ReadingList;
import edu.ap.bib_backend.jpa.entity.ReadingListBook;
import edu.ap.bib_backend.jpa.entity.ReadingListTargetType;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.repository.ReadingListBookRepository;
import edu.ap.bib_backend.jpa.repository.ReadingListRepository;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReadingListServiceTest {

    private static final String USER_ID = "user-1";
    private static final String CLASS_CODE = "2ITSOF1";
    private static final String YEAR_CODE = "2";
    private static final String SCHOOL_CODE = "https://aphogeschool.smartschool.be";

    @Mock
    private ReadingListRepository readingListRepository;

    @Mock
    private ReadingListBookRepository readingListBookRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReadingListService readingListService;

    @BeforeEach
    void setUp() {
        lenient().when(userRepository.findById(USER_ID)).thenReturn(Optional.of(makeUser()));
        setAuthenticationAsStudent();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void givenValidName_whenCreateReadingList_thenReturnCreatedPersonalList() {
        ReadingListDTO dto = new ReadingListDTO("My Reading List", null, null);

        ReadingList savedList = new ReadingList();
        savedList.setId(10L);
        savedList.setName("My Reading List");
        savedList.setTargetType(ReadingListTargetType.PERSONAL);

        when(readingListRepository.save(any(ReadingList.class))).thenReturn(savedList);

        ReadingList result = readingListService.createReadingList(USER_ID, dto);

        assertNotNull(result);
        assertEquals("My Reading List", result.getName());
        verify(readingListRepository).save(argThat(list ->
                "My Reading List".equals(list.getName())
                        && list.getTargetType() == ReadingListTargetType.PERSONAL
                        && list.getTargetValue() == null));
    }

    @Test
    void givenEmptyName_whenCreateReadingList_thenThrowException() {
        ReadingListDTO dto = new ReadingListDTO("", null, null);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> readingListService.createReadingList(USER_ID, dto));

        assertEquals("name mag niet null of leeg zijn", exception.getMessage());
        verify(readingListRepository, never()).save(any());
    }

    @Test
    void givenNoSharedLists_whenGetAllLists_thenAutoCreateClassYearSchoolAndSystem() {
        when(readingListRepository.findFirstByTargetTypeAndTargetValueIgnoreCase(ReadingListTargetType.CLASS, CLASS_CODE))
                .thenReturn(Optional.empty());
        when(readingListRepository.findFirstByTargetTypeAndTargetValueIgnoreCase(ReadingListTargetType.YEAR, YEAR_CODE))
                .thenReturn(Optional.empty());
        when(readingListRepository.findFirstByTargetTypeAndTargetValueIgnoreCase(ReadingListTargetType.SCHOOL, SCHOOL_CODE))
                .thenReturn(Optional.empty());
        when(readingListRepository.findFirstByTargetType(ReadingListTargetType.SYSTEM))
                .thenReturn(Optional.empty());
        when(readingListRepository.findVisibleForUser(USER_ID, CLASS_CODE, YEAR_CODE, SCHOOL_CODE, false))
                .thenReturn(List.of());

        readingListService.getAllLists(USER_ID);

        verify(readingListRepository, times(4)).save(any(ReadingList.class));
        verify(readingListRepository).save(argThat(list ->
                list.getTargetType() == ReadingListTargetType.CLASS
                        && CLASS_CODE.equals(list.getTargetValue())
                        && list.getName().startsWith("Klas ")));
        verify(readingListRepository).save(argThat(list ->
                list.getTargetType() == ReadingListTargetType.YEAR
                        && YEAR_CODE.equals(list.getTargetValue())
                        && list.getName().startsWith("Jaar ")));
        verify(readingListRepository).save(argThat(list ->
                list.getTargetType() == ReadingListTargetType.SCHOOL
                        && SCHOOL_CODE.equals(list.getTargetValue())));
        verify(readingListRepository).save(argThat(list ->
                list.getTargetType() == ReadingListTargetType.SYSTEM
                        && list.getTargetValue() == null));
    }

    @Test
    void givenExistingVisibleList_whenGetListById_thenReturnList() {
        Long id = 10L;
        ReadingList list = new ReadingList();
        list.setId(id);
        list.setName("Test lijst");

        when(readingListRepository.findVisibleById(id, USER_ID, CLASS_CODE, YEAR_CODE, SCHOOL_CODE, false))
                .thenReturn(Optional.of(list));

        ReadingList result = readingListService.getListById(USER_ID, id);

        assertNotNull(result);
        assertEquals("Test lijst", result.getName());
    }

    @Test
    void givenPersonalList_whenDeleteReadingList_thenDeleteSuccessfully() {
        Long id = 10L;
        ReadingList list = new ReadingList();
        list.setId(id);
        list.setName("Gewone lijst");
        list.setTargetType(ReadingListTargetType.PERSONAL);

        when(readingListRepository.findVisibleById(id, USER_ID, CLASS_CODE, YEAR_CODE, SCHOOL_CODE, false))
                .thenReturn(Optional.of(list));

        readingListService.deleteReadingList(USER_ID, id);

        verify(readingListRepository).deleteById(id);
    }

    @Test
    void givenNonPersonalList_whenDeleteReadingList_thenThrowException() {
        Long id = 12L;
        ReadingList list = new ReadingList();
        list.setId(id);
        list.setName("Klaslijst");
        list.setTargetType(ReadingListTargetType.CLASS);

        when(readingListRepository.findVisibleById(id, USER_ID, CLASS_CODE, YEAR_CODE, SCHOOL_CODE, false))
                .thenReturn(Optional.of(list));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> readingListService.deleteReadingList(USER_ID, id));

        assertEquals("Alleen leerkrachten kunnen deze leeslijst verwijderen", exception.getMessage());
        verify(readingListRepository, never()).deleteById(any());
    }

    @Test
    void givenSharedListAndTeacher_whenDeleteReadingList_thenAllow() {
        setAuthenticationAsTeacher();

        Long id = 13L;
        ReadingList list = new ReadingList();
        list.setId(id);
        list.setName("Klaslijst");
        list.setTargetType(ReadingListTargetType.CLASS);

        when(readingListRepository.findVisibleById(id, USER_ID, CLASS_CODE, YEAR_CODE, SCHOOL_CODE, true))
                .thenReturn(Optional.of(list));

        readingListService.deleteReadingList(USER_ID, id);

        verify(readingListRepository).deleteById(id);
    }

    @Test
    void givenPersonalList_whenAddBookToList_thenSaveAndReturn() {
        Long listId = 1L;
        Long bookId = 100L;

        ReadingList list = new ReadingList();
        list.setId(listId);
        list.setTargetType(ReadingListTargetType.PERSONAL);

        ReadingListBook savedBook = new ReadingListBook();
        savedBook.setId(10L);
        savedBook.setReadingList(list);
        savedBook.setBookId(bookId);

        when(readingListRepository.findVisibleById(listId, USER_ID, CLASS_CODE, YEAR_CODE, SCHOOL_CODE, false))
                .thenReturn(Optional.of(list));
        when(readingListBookRepository.existsByReadingList_IdAndBookId(listId, bookId)).thenReturn(false);
        when(readingListBookRepository.save(any(ReadingListBook.class))).thenReturn(savedBook);

        ReadingListBook result = readingListService.addBookToList(USER_ID, listId, bookId);

        assertEquals(bookId, result.getBookId());
        verify(readingListBookRepository).save(any(ReadingListBook.class));
    }

    @Test
    void givenSharedList_whenAddBookToList_thenThrowException() {
        Long listId = 1L;
        Long bookId = 100L;

        ReadingList list = new ReadingList();
        list.setId(listId);
        list.setTargetType(ReadingListTargetType.CLASS);

        when(readingListRepository.findVisibleById(listId, USER_ID, CLASS_CODE, YEAR_CODE, SCHOOL_CODE, false))
                .thenReturn(Optional.of(list));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> readingListService.addBookToList(USER_ID, listId, bookId));

        assertEquals("Alleen leerkrachten kunnen deze leeslijst aanpassen", exception.getMessage());
        verify(readingListBookRepository, never()).save(any());
    }

    @Test
    void givenSharedListAndTeacher_whenAddBookToList_thenAllow() {
        setAuthenticationAsTeacher();

        Long listId = 2L;
        Long bookId = 101L;
        ReadingList list = new ReadingList();
        list.setId(listId);
        list.setTargetType(ReadingListTargetType.YEAR);

        ReadingListBook savedBook = new ReadingListBook();
        savedBook.setId(11L);
        savedBook.setReadingList(list);
        savedBook.setBookId(bookId);

        when(readingListRepository.findVisibleById(listId, USER_ID, CLASS_CODE, YEAR_CODE, SCHOOL_CODE, true))
                .thenReturn(Optional.of(list));
        when(readingListBookRepository.existsByReadingList_IdAndBookId(listId, bookId)).thenReturn(false);
        when(readingListBookRepository.save(any(ReadingListBook.class))).thenReturn(savedBook);

        ReadingListBook result = readingListService.addBookToList(USER_ID, listId, bookId);

        assertEquals(bookId, result.getBookId());
        verify(readingListBookRepository).save(any(ReadingListBook.class));
    }

    @Test
    void givenSystemList_whenAddBookToList_thenAlwaysThrow() {
        setAuthenticationAsTeacher();

        Long listId = 3L;
        ReadingList list = new ReadingList();
        list.setId(listId);
        list.setTargetType(ReadingListTargetType.SYSTEM);

        when(readingListRepository.findVisibleById(listId, USER_ID, CLASS_CODE, YEAR_CODE, SCHOOL_CODE, true))
                .thenReturn(Optional.of(list));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> readingListService.addBookToList(USER_ID, listId, 102L));

        assertEquals("De systeembrede leeslijst kan niet aangepast worden", exception.getMessage());
        verify(readingListBookRepository, never()).save(any());
    }

    @Test
    void givenFavoritesMissing_whenAddBookToFavorites_thenCreateAndAdd() {
        Long bookId = 100L;

        ReadingList favoritesList = new ReadingList();
        favoritesList.setId(99L);
        favoritesList.setName("Favorieten");
        favoritesList.setTargetType(ReadingListTargetType.PERSONAL);

        ReadingListBook savedBook = new ReadingListBook();
        savedBook.setBookId(bookId);
        savedBook.setReadingList(favoritesList);

        when(readingListRepository.findByNameIgnoreCaseAndUser_IdAndTargetType(
                "Favorieten", USER_ID, ReadingListTargetType.PERSONAL)).thenReturn(Optional.empty());
        when(readingListRepository.save(any(ReadingList.class))).thenReturn(favoritesList);
        when(readingListRepository.findVisibleById(99L, USER_ID, CLASS_CODE, YEAR_CODE, SCHOOL_CODE, false))
                .thenReturn(Optional.of(favoritesList));
        when(readingListBookRepository.existsByReadingList_IdAndBookId(99L, bookId)).thenReturn(false);
        when(readingListBookRepository.save(any(ReadingListBook.class))).thenReturn(savedBook);

        ReadingListBook result = readingListService.addBookToFavorites(USER_ID, bookId);

        assertEquals(bookId, result.getBookId());
        verify(readingListRepository).save(any(ReadingList.class));
    }

    @Test
    void givenSharedList_whenRemoveBookFromList_thenThrowException() {
        Long listId = 10L;

        ReadingList sharedList = new ReadingList();
        sharedList.setId(listId);
        sharedList.setTargetType(ReadingListTargetType.YEAR);

        when(readingListRepository.findVisibleById(listId, USER_ID, CLASS_CODE, YEAR_CODE, SCHOOL_CODE, false))
                .thenReturn(Optional.of(sharedList));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> readingListService.removeBookFromList(USER_ID, listId, 100L));

        assertEquals("Alleen leerkrachten kunnen deze leeslijst aanpassen", exception.getMessage());
        verify(readingListBookRepository, never()).deleteAllByReadingListIdAndBookId(anyLong(), anyLong());
    }

    @Test
    void givenSharedListAndTeacher_whenRemoveBookFromList_thenAllow() {
        setAuthenticationAsTeacher();

        Long listId = 11L;
        Long bookId = 103L;

        ReadingList sharedList = new ReadingList();
        sharedList.setId(listId);
        sharedList.setTargetType(ReadingListTargetType.SCHOOL);

        when(readingListRepository.findVisibleById(listId, USER_ID, CLASS_CODE, YEAR_CODE, SCHOOL_CODE, true))
                .thenReturn(Optional.of(sharedList));
        when(readingListBookRepository.deleteAllByReadingListIdAndBookId(listId, bookId)).thenReturn(1);

        readingListService.removeBookFromList(USER_ID, listId, bookId);

        verify(readingListBookRepository).deleteAllByReadingListIdAndBookId(listId, bookId);
    }

    @Test
    void givenSystemList_whenRemoveBookFromList_thenAlwaysThrow() {
        setAuthenticationAsTeacher();

        Long listId = 12L;
        ReadingList systemList = new ReadingList();
        systemList.setId(listId);
        systemList.setTargetType(ReadingListTargetType.SYSTEM);

        when(readingListRepository.findVisibleById(listId, USER_ID, CLASS_CODE, YEAR_CODE, SCHOOL_CODE, true))
                .thenReturn(Optional.of(systemList));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> readingListService.removeBookFromList(USER_ID, listId, 104L));

        assertEquals("De systeembrede leeslijst kan niet aangepast worden", exception.getMessage());
        verify(readingListBookRepository, never()).deleteAllByReadingListIdAndBookId(anyLong(), anyLong());
    }

    private void setAuthenticationAsStudent() {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(USER_ID, null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
        applyAudienceDetails(authentication);
    }

    private void setAuthenticationAsTeacher() {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(USER_ID, null, List.of(new SimpleGrantedAuthority("ROLE_TEACHER")));
        applyAudienceDetails(authentication);
    }

    private void applyAudienceDetails(UsernamePasswordAuthenticationToken authentication) {
        Map<String, Object> details = new HashMap<>();
        details.put("classCode", CLASS_CODE);
        details.put("yearCode", YEAR_CODE);
        details.put("schoolCode", SCHOOL_CODE);
        authentication.setDetails(details);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private User makeUser() {
        User user = new User();
        user.setId(USER_ID);
        return user;
    }
}
