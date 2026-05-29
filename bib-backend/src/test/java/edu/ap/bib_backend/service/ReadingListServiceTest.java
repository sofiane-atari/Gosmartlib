package edu.ap.bib_backend.service;

import edu.ap.bib_backend.dto.ReadingListDTO;
import edu.ap.bib_backend.jpa.entity.Campus;
import edu.ap.bib_backend.jpa.entity.ReadingList;
import edu.ap.bib_backend.jpa.entity.ReadingListBook;
import edu.ap.bib_backend.jpa.entity.ReadingListTargetType;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.repository.ReadingListBookRepository;
import edu.ap.bib_backend.jpa.repository.ReadingListRepository;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import edu.ap.bib_backend.service.user.CurrentUserService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReadingListServiceTest {

        private static final String USER_ID = "user-1";
        private static final String CLASS_CODE = "2ITSOF1";
        private static final String YEAR_CODE = "2";
        private static final String SCHOOL_CODE = "https://aphogeschool.smartschool.be";
	private static final Long CAMPUS_ID = 100L;
	private static final Long SCHOOL_ID = 200L;

        @Mock
        private ReadingListRepository readingListRepository;

        @Mock
        private ReadingListBookRepository readingListBookRepository;

        @Mock
        private UserRepository userRepository;

        @Mock
        private CurrentUserService currentUserService;

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
                savedList.setCampus(makeCampus());

                when(readingListRepository.save(any(ReadingList.class))).thenReturn(savedList);

                ReadingList result = readingListService.createReadingList(USER_ID, dto);

                assertNotNull(result);
                assertEquals("My Reading List", result.getName());

                verify(readingListRepository).save(argThat(list -> "My Reading List".equals(list.getName())
                                && list.getTargetType() == ReadingListTargetType.PERSONAL
                                && list.getTargetValue() == null
                                && list.getCampus() != null
                                && CAMPUS_ID.equals(list.getCampus().getId())));
        }

        @Test
        void givenEmptyName_whenCreateReadingList_thenThrowException() {
                ReadingListDTO dto = new ReadingListDTO("", null, null);

                IllegalArgumentException exception = assertThrows(
                                IllegalArgumentException.class,
                                () -> readingListService.createReadingList(USER_ID, dto));

                assertEquals("name mag niet null of leeg zijn", exception.getMessage());
                verify(readingListRepository, never()).save(any());
        }

        @Test
        void givenNoSharedLists_whenGetAllLists_thenAutoCreateClassAndSystem() {
                when(readingListRepository.findFirstByTargetTypeAndTargetValueIgnoreCaseAndCampusId(
                                ReadingListTargetType.CLASS,
                                CLASS_CODE,
                                CAMPUS_ID)).thenReturn(Optional.empty());

                when(readingListRepository.findFirstByTargetTypeAndCampus_School_Id(
                                ReadingListTargetType.SYSTEM,
                                SCHOOL_ID)).thenReturn(Optional.empty());

                when(readingListRepository.findVisibleForUser(
                                USER_ID,
                                CAMPUS_ID,
                                CLASS_CODE,
                                YEAR_CODE,
                                SCHOOL_CODE,
                                false)).thenReturn(List.of());

                readingListService.getAllLists(USER_ID);

                verify(readingListRepository, times(2)).save(any(ReadingList.class));

                verify(readingListRepository).save(argThat(list -> list.getTargetType() == ReadingListTargetType.CLASS
                                && CLASS_CODE.equals(list.getTargetValue())
                                && list.getName().startsWith("Klas ")
                                && list.getCampus() != null
                                && CAMPUS_ID.equals(list.getCampus().getId())));

                verify(readingListRepository).save(argThat(list -> list.getTargetType() == ReadingListTargetType.SYSTEM
                                && list.getTargetValue() == null
                                && list.getCampus() != null
                                && CAMPUS_ID.equals(list.getCampus().getId())));
        }

        @Test
        void givenExistingVisibleList_whenGetListById_thenReturnList() {
                Long id = 10L;

                ReadingList list = new ReadingList();
                list.setId(id);
                list.setName("Test lijst");
                list.setCampus(makeCampus());

                when(readingListRepository.findVisibleById(
                                id,
                                USER_ID,
                                CAMPUS_ID,
                                CLASS_CODE,
                                YEAR_CODE,
                                SCHOOL_CODE,
                                false)).thenReturn(Optional.of(list));

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
                list.setCampus(makeCampus());

                when(readingListRepository.findVisibleById(
                                id,
                                USER_ID,
                                CAMPUS_ID,
                                CLASS_CODE,
                                YEAR_CODE,
                                SCHOOL_CODE,
                                false)).thenReturn(Optional.of(list));

                readingListService.deleteReadingList(USER_ID, id);

                verify(readingListRepository).deleteById(id);
        }

        @Test
        void givenNonPersonalListAndStudent_whenDeleteReadingList_thenThrowException() {
                Long id = 12L;

                ReadingList list = new ReadingList();
                list.setId(id);
                list.setName("Klaslijst");
                list.setTargetType(ReadingListTargetType.CLASS);
                list.setCampus(makeCampus());

                when(readingListRepository.findVisibleById(
                                id,
                                USER_ID,
                                CAMPUS_ID,
                                CLASS_CODE,
                                YEAR_CODE,
                                SCHOOL_CODE,
                                false)).thenReturn(Optional.of(list));

                IllegalArgumentException exception = assertThrows(
                                IllegalArgumentException.class,
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
                list.setCampus(makeCampus());

                when(readingListRepository.findVisibleById(
                                id,
                                USER_ID,
                                CAMPUS_ID,
                                CLASS_CODE,
                                YEAR_CODE,
                                SCHOOL_CODE,
                                true)).thenReturn(Optional.of(list));

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
                list.setCampus(makeCampus());

                ReadingListBook savedBook = new ReadingListBook();
                savedBook.setId(10L);
                savedBook.setReadingList(list);
                savedBook.setBookId(bookId);

                when(readingListRepository.findVisibleById(
                                listId,
                                USER_ID,
                                CAMPUS_ID,
                                CLASS_CODE,
                                YEAR_CODE,
                                SCHOOL_CODE,
                                false)).thenReturn(Optional.of(list));

                when(readingListBookRepository.existsByReadingList_IdAndBookId(listId, bookId)).thenReturn(false);
                when(readingListBookRepository.save(any(ReadingListBook.class))).thenReturn(savedBook);

                ReadingListBook result = readingListService.addBookToList(USER_ID, listId, bookId);

                assertEquals(bookId, result.getBookId());
                verify(readingListBookRepository).save(any(ReadingListBook.class));
        }

        @Test
        void givenSharedListAndStudent_whenAddBookToList_thenThrowException() {
                Long listId = 1L;
                Long bookId = 100L;

                ReadingList list = new ReadingList();
                list.setId(listId);
                list.setTargetType(ReadingListTargetType.CLASS);
                list.setCampus(makeCampus());

                when(readingListRepository.findVisibleById(
                                listId,
                                USER_ID,
                                CAMPUS_ID,
                                CLASS_CODE,
                                YEAR_CODE,
                                SCHOOL_CODE,
                                false)).thenReturn(Optional.of(list));

                IllegalArgumentException exception = assertThrows(
                                IllegalArgumentException.class,
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
                list.setTargetType(ReadingListTargetType.CLASS);
                list.setCampus(makeCampus());

                ReadingListBook savedBook = new ReadingListBook();
                savedBook.setId(11L);
                savedBook.setReadingList(list);
                savedBook.setBookId(bookId);

                when(readingListRepository.findVisibleById(
                                listId,
                                USER_ID,
                                CAMPUS_ID,
                                CLASS_CODE,
                                YEAR_CODE,
                                SCHOOL_CODE,
                                true)).thenReturn(Optional.of(list));

                when(readingListBookRepository.existsByReadingList_IdAndBookId(listId, bookId)).thenReturn(false);
                when(readingListBookRepository.save(any(ReadingListBook.class))).thenReturn(savedBook);

                ReadingListBook result = readingListService.addBookToList(USER_ID, listId, bookId);

                assertEquals(bookId, result.getBookId());
                verify(readingListBookRepository).save(any(ReadingListBook.class));
        }

        @Test
        void givenSystemListAndStudent_whenAddBookToList_thenThrowException() {
                Long listId = 3L;

                ReadingList list = new ReadingList();
                list.setId(listId);
                list.setTargetType(ReadingListTargetType.SYSTEM);
                list.setCampus(makeCampus());

                when(readingListRepository.findVisibleById(
                                listId,
                                USER_ID,
                                CAMPUS_ID,
                                CLASS_CODE,
                                YEAR_CODE,
                                SCHOOL_CODE,
                                false)).thenReturn(Optional.of(list));

                IllegalArgumentException exception = assertThrows(
                                IllegalArgumentException.class,
                                () -> readingListService.addBookToList(USER_ID, listId, 102L));

                assertEquals("De systeembrede leeslijst kan niet aangepast worden", exception.getMessage());
                verify(readingListBookRepository, never()).save(any());
        }

        @Test
        void givenSystemListAndLibrarian_whenAddBookToList_thenAllow() {
                setAuthenticationAsLibrarian();

                Long listId = 3L;
                Long bookId = 102L;

                ReadingList list = new ReadingList();
                list.setId(listId);
                list.setTargetType(ReadingListTargetType.SYSTEM);
                list.setCampus(makeCampus());

                ReadingListBook savedBook = new ReadingListBook();
                savedBook.setId(12L);
                savedBook.setReadingList(list);
                savedBook.setBookId(bookId);

                when(readingListRepository.findVisibleById(
                                listId,
                                USER_ID,
                                CAMPUS_ID,
                                CLASS_CODE,
                                YEAR_CODE,
                                SCHOOL_CODE,
                                true)).thenReturn(Optional.of(list));

                when(readingListBookRepository.existsByReadingList_IdAndBookId(listId, bookId)).thenReturn(false);
                when(readingListBookRepository.save(any(ReadingListBook.class))).thenReturn(savedBook);

                ReadingListBook result = readingListService.addBookToList(USER_ID, listId, bookId);

                assertEquals(bookId, result.getBookId());
                verify(readingListBookRepository).save(any(ReadingListBook.class));
        }

        @Test
        void givenFavoritesMissing_whenAddBookToFavorites_thenCreateAndAdd() {
                Long bookId = 100L;

                ReadingList favoritesList = new ReadingList();
                favoritesList.setId(99L);
                favoritesList.setName("Favorieten");
                favoritesList.setTargetType(ReadingListTargetType.PERSONAL);
                favoritesList.setCampus(makeCampus());

                ReadingListBook savedBook = new ReadingListBook();
                savedBook.setBookId(bookId);
                savedBook.setReadingList(favoritesList);

                when(readingListRepository.findByNameIgnoreCaseAndUser_IdAndTargetTypeAndCampusId(
                                "Favorieten",
                                USER_ID,
                                ReadingListTargetType.PERSONAL,
                                CAMPUS_ID)).thenReturn(Optional.empty());

                when(readingListRepository.save(any(ReadingList.class))).thenReturn(favoritesList);

                when(readingListRepository.findVisibleById(
                                99L,
                                USER_ID,
                                CAMPUS_ID,
                                CLASS_CODE,
                                YEAR_CODE,
                                SCHOOL_CODE,
                                false)).thenReturn(Optional.of(favoritesList));

                when(readingListBookRepository.existsByReadingList_IdAndBookId(99L, bookId)).thenReturn(false);
                when(readingListBookRepository.save(any(ReadingListBook.class))).thenReturn(savedBook);

                ReadingListBook result = readingListService.addBookToFavorites(USER_ID, bookId);

                assertEquals(bookId, result.getBookId());
                verify(readingListRepository).save(any(ReadingList.class));
        }

        @Test
        void givenSharedListAndStudent_whenRemoveBookFromList_thenThrowException() {
                Long listId = 10L;

                ReadingList sharedList = new ReadingList();
                sharedList.setId(listId);
                sharedList.setTargetType(ReadingListTargetType.CLASS);
                sharedList.setCampus(makeCampus());

                when(readingListRepository.findVisibleById(
                                listId,
                                USER_ID,
                                CAMPUS_ID,
                                CLASS_CODE,
                                YEAR_CODE,
                                SCHOOL_CODE,
                                false)).thenReturn(Optional.of(sharedList));

                IllegalArgumentException exception = assertThrows(
                                IllegalArgumentException.class,
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
                sharedList.setTargetType(ReadingListTargetType.CLASS);
                sharedList.setCampus(makeCampus());

                when(readingListRepository.findVisibleById(
                                listId,
                                USER_ID,
                                CAMPUS_ID,
                                CLASS_CODE,
                                YEAR_CODE,
                                SCHOOL_CODE,
                                true)).thenReturn(Optional.of(sharedList));

                when(readingListBookRepository.deleteAllByReadingListIdAndBookId(listId, bookId)).thenReturn(1);

                readingListService.removeBookFromList(USER_ID, listId, bookId);

                verify(readingListBookRepository).deleteAllByReadingListIdAndBookId(listId, bookId);
        }

        @Test
        void givenSystemListAndStudent_whenRemoveBookFromList_thenThrowException() {
                Long listId = 12L;

                ReadingList systemList = new ReadingList();
                systemList.setId(listId);
                systemList.setTargetType(ReadingListTargetType.SYSTEM);
                systemList.setCampus(makeCampus());

                when(readingListRepository.findVisibleById(
                                listId,
                                USER_ID,
                                CAMPUS_ID,
                                CLASS_CODE,
                                YEAR_CODE,
                                SCHOOL_CODE,
                                false)).thenReturn(Optional.of(systemList));

                IllegalArgumentException exception = assertThrows(
                                IllegalArgumentException.class,
                                () -> readingListService.removeBookFromList(USER_ID, listId, 104L));

                assertEquals("De systeembrede leeslijst kan niet aangepast worden", exception.getMessage());
                verify(readingListBookRepository, never()).deleteAllByReadingListIdAndBookId(anyLong(), anyLong());
        }

        @Test
        void givenSystemListAndLibrarian_whenRemoveBookFromList_thenAllow() {
                setAuthenticationAsLibrarian();

                Long listId = 12L;
                Long bookId = 104L;

                ReadingList systemList = new ReadingList();
                systemList.setId(listId);
                systemList.setTargetType(ReadingListTargetType.SYSTEM);
                systemList.setCampus(makeCampus());

                when(readingListRepository.findVisibleById(
                                listId,
                                USER_ID,
                                CAMPUS_ID,
                                CLASS_CODE,
                                YEAR_CODE,
                                SCHOOL_CODE,
                                true)).thenReturn(Optional.of(systemList));

                when(readingListBookRepository.deleteAllByReadingListIdAndBookId(listId, bookId)).thenReturn(1);

                readingListService.removeBookFromList(USER_ID, listId, bookId);

                verify(readingListBookRepository).deleteAllByReadingListIdAndBookId(listId, bookId);
        }

        private void setAuthenticationAsStudent() {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                USER_ID,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));

                applyAudienceDetails(authentication);
                mockCurrentUserAuthorities("ROLE_STUDENT");
        }

        private void setAuthenticationAsLibrarian() {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                USER_ID,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_LIBRARIAN")));

                applyAudienceDetails(authentication);
                mockCurrentUserAuthorities("ROLE_LIBRARIAN");
        }

        private void setAuthenticationAsTeacher() {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                USER_ID,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_TEACHER")));

                applyAudienceDetails(authentication);
                mockCurrentUserAuthorities("ROLE_TEACHER");
        }

        private void applyAudienceDetails(UsernamePasswordAuthenticationToken authentication) {
                Map<String, Object> details = new HashMap<>();
                details.put("classCode", CLASS_CODE);
                details.put("yearCode", YEAR_CODE);
                details.put("schoolCode", SCHOOL_CODE);
                authentication.setDetails(details);
                SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        private void mockCurrentUserAuthorities(String... authorities) {
                Set<String> currentAuthorities = Set.of(authorities);

                lenient().doAnswer(invocation -> {
                        Object[] arguments = invocation.getArguments();

                        for (Object argument : arguments) {
                                if (argument instanceof String authority && currentAuthorities.contains(authority)) {
                                        return true;
                                }

                                if (argument instanceof String[] allowedAuthorities) {
                                        for (String allowedAuthority : allowedAuthorities) {
                                                if (currentAuthorities.contains(allowedAuthority)) {
                                                        return true;
                                                }
                                        }
                                }
                        }

                        return false;
                }).when(currentUserService).currentUserHasAnyAuthority(any(String[].class));
        }

        private User makeUser() {
                School school = makeSchool();
                User user = new User();
                user.setId(USER_ID);
                user.setCampus(makeCampus());
                user.setSchool(school);
                return user;
        }

        private Campus makeCampus() {
                Campus campus = new Campus();
                campus.setId(CAMPUS_ID);
                campus.setName("Test Campus");
                campus.setSchool(makeSchool());
                return campus;
        }

        private School makeSchool() {
                School school = new School();
                school.setId(SCHOOL_ID);
                school.setName("Test School");
                return school;
        }
}