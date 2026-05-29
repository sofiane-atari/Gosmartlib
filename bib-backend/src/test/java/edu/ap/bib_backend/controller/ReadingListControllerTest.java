package edu.ap.bib_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ap.bib_backend.dto.AddBookDTO;
import edu.ap.bib_backend.dto.ReadingListDTO;
import edu.ap.bib_backend.jpa.entity.Campus;
import edu.ap.bib_backend.jpa.entity.ReadingList;
import edu.ap.bib_backend.jpa.entity.ReadingListBook;
import edu.ap.bib_backend.jpa.entity.ReadingListTargetType;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.service.JwtService;
import edu.ap.bib_backend.service.ReadingListService;
import edu.ap.bib_backend.service.oneroster.OneRosterAudienceService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.test.web.servlet.setup.MockMvcBuilders;


class ReadingListControllerTest {

        private static final String USER_ID = "user-1";

        private MockMvc mockMvc;
        private ObjectMapper objectMapper;
        private ReadingListService readingListService;
        private OneRosterAudienceService oneRosterAudienceService;
        private JwtService jwtService;

        @BeforeEach
        void setUp() {
                readingListService = mock(ReadingListService.class);
                oneRosterAudienceService = mock(OneRosterAudienceService.class);
                jwtService = mock(JwtService.class);
                objectMapper = new ObjectMapper();

                ReadingListController controller = new ReadingListController(
                                readingListService,
                                oneRosterAudienceService,
                                jwtService);

                mockMvc = MockMvcBuilders
                                .standaloneSetup(controller)
                                .setControllerAdvice(new TestExceptionHandler())
                                .build();
        }

        @Test
        void givenExistingLists_whenGetAllReadingLists_thenReturnOkWithLists() throws Exception {
                ReadingList list = new ReadingList();
                list.setId(10L);
                list.setName("Test List");

                when(readingListService.getAllLists(USER_ID)).thenReturn(List.of(list));

                mockMvc.perform(get("/reading-lists").principal(userAuthentication()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].name").value("Test List"))
                                .andExpect(jsonPath("$[0].id").value(10));
        }

        @Test
        void givenNoLists_whenGetAllReadingLists_thenReturnEmptyArray() throws Exception {
                when(readingListService.getAllLists(USER_ID)).thenReturn(List.of());

                mockMvc.perform(get("/reading-lists").principal(userAuthentication()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        void givenValidDTO_whenCreateReadingList_thenReturnCreated() throws Exception {
                ReadingListDTO dto = new ReadingListDTO("New List", null, null);

                ReadingList savedList = new ReadingList();
                savedList.setId(10L);
                savedList.setName("New List");

                when(readingListService.createReadingList(eq(USER_ID), any(ReadingListDTO.class)))
                                .thenReturn(savedList);

                mockMvc.perform(post("/reading-lists")
                                .principal(userAuthentication())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.name").value("New List"))
                                .andExpect(jsonPath("$.id").value(10));
        }

        @Test
        void givenEmptyName_whenCreateReadingList_thenReturnBadRequest() throws Exception {
                ReadingListDTO dto = new ReadingListDTO("", null, null);

                when(readingListService.createReadingList(eq(USER_ID), any(ReadingListDTO.class)))
                                .thenThrow(new IllegalArgumentException("name mag niet null of leeg zijn"));

                mockMvc.perform(post("/reading-lists")
                                .principal(userAuthentication())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void givenExistingId_whenDeleteReadingList_thenReturnNoContent() throws Exception {
                doNothing().when(readingListService).deleteReadingList(USER_ID, 10L);

                mockMvc.perform(delete("/reading-lists/{id}", 10L)
                                .principal(userAuthentication()))
                                .andExpect(status().isNoContent());

                verify(readingListService).deleteReadingList(USER_ID, 10L);
        }

        @Test
        void givenNonExistingId_whenDeleteReadingList_thenReturnBadRequest() throws Exception {
                doThrow(new IllegalArgumentException("Leeslijst met id 99 bestaat niet"))
                                .when(readingListService).deleteReadingList(USER_ID, 99L);

                mockMvc.perform(delete("/reading-lists/{id}", 99L)
                                .principal(userAuthentication()))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void givenFavoritesList_whenDeleteReadingList_thenReturnBadRequest() throws Exception {
                doThrow(new IllegalArgumentException("Deze leeslijst kan niet verwijderd worden"))
                                .when(readingListService).deleteReadingList(USER_ID, 11L);

                mockMvc.perform(delete("/reading-lists/{id}", 11L)
                                .principal(userAuthentication()))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void givenExistingId_whenGetReadingListById_thenReturnOk() throws Exception {
                ReadingList list = new ReadingList();
                list.setId(10L);
                list.setName("Gedeelde lijst");

                when(readingListService.getListById(USER_ID, 10L)).thenReturn(list);

                mockMvc.perform(get("/reading-lists/{id}", 10L)
                                .principal(userAuthentication()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(10))
                                .andExpect(jsonPath("$.name").value("Gedeelde lijst"));
        }

        @Test
        void givenNonExistingId_whenGetReadingListById_thenReturnBadRequest() throws Exception {
                doThrow(new IllegalArgumentException("Leeslijst met id 99 bestaat niet"))
                                .when(readingListService).getListById(USER_ID, 99L);

                mockMvc.perform(get("/reading-lists/{id}", 99L)
                                .principal(userAuthentication()))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void givenValidData_whenAddBookToList_thenReturnCreated() throws Exception {
                AddBookDTO dto = new AddBookDTO(100L);

                ReadingList list = new ReadingList();
                list.setId(10L);

                ReadingListBook savedBook = new ReadingListBook();
                savedBook.setId(1L);
                savedBook.setBookId(100L);
                savedBook.setReadingList(list);

                when(readingListService.addBookToList(USER_ID, 10L, 100L)).thenReturn(savedBook);

                mockMvc.perform(post("/reading-lists/{listId}/books", 10L)
                                .principal(userAuthentication())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.bookId").value(100));
        }

        @Test
        void givenValidData_whenAddBookToFavorites_thenReturnCreated() throws Exception {
                AddBookDTO dto = new AddBookDTO(100L);

                ReadingList list = new ReadingList();
                list.setId(99L);

                ReadingListBook savedBook = new ReadingListBook();
                savedBook.setId(2L);
                savedBook.setBookId(100L);
                savedBook.setReadingList(list);

                when(readingListService.addBookToFavorites(USER_ID, 100L)).thenReturn(savedBook);

                mockMvc.perform(post("/reading-lists/favorites/books")
                                .principal(userAuthentication())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.bookId").value(100));
        }

        @Test
        void givenBookInList_whenRemoveBookFromList_thenReturnNoContent() throws Exception {
                doNothing().when(readingListService).removeBookFromList(USER_ID, 10L, 100L);

                mockMvc.perform(delete("/reading-lists/{listId}/books/{bookId}", 10L, 100L)
                                .principal(userAuthentication()))
                                .andExpect(status().isNoContent());

                verify(readingListService).removeBookFromList(USER_ID, 10L, 100L);
        }

        @Test
        void givenBookMissing_whenRemoveBookFromList_thenReturnBadRequest() throws Exception {
                doThrow(new IllegalArgumentException("Boek met id 999 zit niet in deze leeslijst"))
                                .when(readingListService).removeBookFromList(USER_ID, 10L, 999L);

                mockMvc.perform(delete("/reading-lists/{listId}/books/{bookId}", 10L, 999L)
                                .principal(userAuthentication()))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void givenUnauthenticatedUser_whenGetAllReadingLists_thenReturn401() throws Exception {
                mockMvc.perform(get("/reading-lists"))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void givenVisibleLists_whenGetAllReadingLists_thenReturnsOkAndLists() throws Exception {
                ReadingList personalList = makeReadingList(
                                1L,
                                "Mijn lijst",
                                ReadingListTargetType.PERSONAL,
                                null,
                                false,
                                null);

                ReadingList systemList = makeReadingList(
                                2L,
                                "Uitgelicht",
                                ReadingListTargetType.SYSTEM,
                                null,
                                true,
                                1);

                when(readingListService.getAllLists(USER_ID)).thenReturn(List.of(personalList, systemList));

                mockMvc.perform(get("/reading-lists").principal(principal()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(2)))
                                .andExpect(jsonPath("$[*].name", hasItem("Mijn lijst")))
                                .andExpect(jsonPath("$[*].name", hasItem("Uitgelicht")))
                                .andExpect(jsonPath("$[1].targetType").value("SYSTEM"))
                                .andExpect(jsonPath("$[1].showOnCatalog").value(true))
                                .andExpect(jsonPath("$[1].catalogFeaturedOrder").value(1));

                verify(readingListService).getAllLists(USER_ID);
        }

        @Test
        void givenListId_whenGetReadingListById_thenReturnsList() throws Exception {
                ReadingList list = makeReadingList(
                                10L,
                                "Detail lijst",
                                ReadingListTargetType.PERSONAL,
                                null,
                                false,
                                null);

                when(readingListService.getListById(USER_ID, 10L)).thenReturn(list);

                mockMvc.perform(get("/reading-lists/{id}", 10L).principal(principal()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(10))
                                .andExpect(jsonPath("$.name").value("Detail lijst"))
                                .andExpect(jsonPath("$.targetType").value("PERSONAL"));

                verify(readingListService).getListById(USER_ID, 10L);
        }

        @Test
        void givenCatalogFeaturedLists_whenGetCatalogFeaturedLists_thenReturnsOnlyCatalogLists() throws Exception {
                ReadingList first = makeReadingList(
                                21L,
                                "Eerste selectie",
                                ReadingListTargetType.SYSTEM,
                                null,
                                true,
                                1);

                ReadingList second = makeReadingList(
                                22L,
                                "Tweede selectie",
                                ReadingListTargetType.SYSTEM,
                                null,
                                true,
                                2);

                when(readingListService.getCatalogFeaturedListsForCurrentUserCampus(USER_ID))
                                .thenReturn(List.of(first, second));

                mockMvc.perform(get("/reading-lists/featured/catalog").principal(principal()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(2)))
                                .andExpect(jsonPath("$[*].name", hasItem("Eerste selectie")))
                                .andExpect(jsonPath("$[*].name", hasItem("Tweede selectie")))
                                .andExpect(jsonPath("$[0].showOnCatalog").value(true))
                                .andExpect(jsonPath("$[0].catalogFeaturedOrder").value(1))
                                .andExpect(jsonPath("$[1].showOnCatalog").value(true))
                                .andExpect(jsonPath("$[1].catalogFeaturedOrder").value(2));

                verify(readingListService).getCatalogFeaturedListsForCurrentUserCampus(USER_ID);
        }

        @Test
        void givenListId_whenSelectCatalogFeaturedList_thenUsesPathVariableAndReturnsSelectedLists() throws Exception {
                ReadingList selected = makeReadingList(
                                30L,
                                "Nieuwe selectie",
                                ReadingListTargetType.SYSTEM,
                                null,
                                true,
                                1);

                when(readingListService.selectFeaturedListForCatalog(USER_ID, 30L))
                                .thenReturn(List.of(selected));

                mockMvc.perform(post("/reading-lists/{id}/catalog-featured", 30L).principal(principal()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(1)))
                                .andExpect(jsonPath("$[0].id").value(30))
                                .andExpect(jsonPath("$[0].name").value("Nieuwe selectie"))
                                .andExpect(jsonPath("$[0].showOnCatalog").value(true))
                                .andExpect(jsonPath("$[0].catalogFeaturedOrder").value(1));

                verify(readingListService).selectFeaturedListForCatalog(USER_ID, 30L);
        }

        @Test
        void givenListId_whenUnselectCatalogFeaturedList_thenUsesPathVariableAndReturnsRemainingSelectedLists()
                        throws Exception {
                ReadingList remaining = makeReadingList(
                                41L,
                                "Overblijvende selectie",
                                ReadingListTargetType.SYSTEM,
                                null,
                                true,
                                1);

                when(readingListService.unselectFeaturedListForCatalog(USER_ID, 40L))
                                .thenReturn(List.of(remaining));

                mockMvc.perform(delete("/reading-lists/{id}/catalog-featured", 40L).principal(principal()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(1)))
                                .andExpect(jsonPath("$[0].id").value(41))
                                .andExpect(jsonPath("$[0].name").value("Overblijvende selectie"))
                                .andExpect(jsonPath("$[0].showOnCatalog").value(true));

                verify(readingListService).unselectFeaturedListForCatalog(USER_ID, 40L);
        }

        @Test
        void givenSharedList_whenGetSharedReadingList_thenReturnsSharedList() throws Exception {
                ReadingList sharedList = makeReadingList(
                                50L,
                                "Gedeelde lijst",
                                ReadingListTargetType.PERSONAL,
                                null,
                                false,
                                null);

                sharedList.setPublicShareId("share-123");
                sharedList.setShareEnabled(true);

                when(readingListService.getSharedList("share-123")).thenReturn(sharedList);

                mockMvc.perform(get("/reading-lists/shared/{publicShareId}", "share-123").principal(principal()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(50))
                                .andExpect(jsonPath("$.name").value("Gedeelde lijst"))
                                .andExpect(jsonPath("$.publicShareId").value("share-123"))
                                .andExpect(jsonPath("$.shareEnabled").value(true));

                verify(readingListService).getSharedList("share-123");
        }

        @Test
        void givenListWithBooks_whenGetReadingListById_thenMapsBooksInResponse() throws Exception {
                ReadingList list = makeReadingList(
                                60L,
                                "Lijst met boeken",
                                ReadingListTargetType.PERSONAL,
                                null,
                                false,
                                null);

                ReadingListBook firstBook = makeReadingListBook(1L, list, 100L);
                ReadingListBook secondBook = makeReadingListBook(2L, list, 101L);
                list.setBooks(new ArrayList<>(List.of(firstBook, secondBook)));

                when(readingListService.getListById(USER_ID, 60L)).thenReturn(list);

                mockMvc.perform(get("/reading-lists/{id}", 60L).principal(principal()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(60))
                                .andExpect(jsonPath("$.books", hasSize(2)))
                                .andExpect(jsonPath("$.books[0].bookId").value(100))
                                .andExpect(jsonPath("$.books[1].bookId").value(101));

                verify(readingListService).getListById(USER_ID, 60L);
        }

        private UsernamePasswordAuthenticationToken principal() {
                return new UsernamePasswordAuthenticationToken(USER_ID, null, List.of());
        }

        private ReadingList makeReadingList(
                        Long id,
                        String name,
                        ReadingListTargetType targetType,
                        String targetValue,
                        boolean showOnCatalog,
                        Integer catalogFeaturedOrder) {
                Campus campus = new Campus();
                campus.setId(100L);
                campus.setName("Test Campus");

                User user = new User();
                user.setId(USER_ID);
                user.setSchool(campus.getSchool());

                ReadingList list = new ReadingList();
                list.setId(id);
                list.setName(name);
                list.setUser(user);
                list.setCampus(campus);
                list.setTargetType(targetType);
                list.setTargetValue(targetValue);
                list.setShowOnCatalog(showOnCatalog);
                list.setCatalogFeaturedOrder(catalogFeaturedOrder);
                list.setBooks(new ArrayList<>());

                return list;
        }

        private ReadingListBook makeReadingListBook(Long id, ReadingList list, Long bookId) {
                ReadingListBook book = new ReadingListBook();
                book.setId(id);
                book.setReadingList(list);
                book.setBookId(bookId);

                return book;
        }

        private Authentication userAuthentication() {
                return new UsernamePasswordAuthenticationToken(USER_ID, null, List.of());
        }
}