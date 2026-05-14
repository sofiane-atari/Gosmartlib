package edu.ap.bib_backend.service.book;

import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.BookCopy;
import edu.ap.bib_backend.jpa.entity.Campus;
import edu.ap.bib_backend.jpa.entity.Genre;
import edu.ap.bib_backend.jpa.entity.ReadingLevel;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.repository.BookCopyRepository;
import edu.ap.bib_backend.jpa.repository.BookRepository;
import edu.ap.bib_backend.jpa.repository.GenreRepository;
import edu.ap.bib_backend.jpa.repository.ReadingLevelRepository;
import edu.ap.bib_backend.jpa.repository.UserRepository;

import edu.ap.bib_backend.service.user.CurrentUserService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BookBulkServiceTest {

    private BookRepository br;
    private BookCopyRepository bcr;
    private BookBulkService service;
    private GenreRepository gr;
    private ReadingLevelRepository rlr;
    private CurrentUserService currentUserService;
    private Authentication authentication;
    private School school;
    private Campus campus;

    @BeforeEach
    void setUp() throws IOException {
        br = mock(BookRepository.class);
        bcr = mock(BookCopyRepository.class);
        gr = mock(GenreRepository.class);
        rlr = mock(ReadingLevelRepository.class);
        currentUserService = mock(CurrentUserService.class);
        service = new BookBulkService(br, bcr, gr, rlr, currentUserService);

        authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("user-1");

        school = new School();
        school.setId(1L);
        school.setDomainName("testschool.be");

        User user = new User();
        user.setId("user-1");
        user.setSchool(school);
        when(currentUserService.extractCurrentUserId(authentication)).thenReturn("user-1");

        campus = new Campus();
        campus.setId(1L);
        when(currentUserService.getCurrentUserCampus(authentication)).thenReturn(campus);

        Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "book-uploads");
        if (!Files.exists(tempDir)) {
            Files.createDirectories(tempDir);
        }
    }

    @Test
    @DisplayName("storeTempFile moet een temp file opslaan en id teruggeven")
    void storeTempFile_ShouldStoreFile() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "books.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "excel-content".getBytes());

        String tempFileId = service.storeTempFile(file);

        assertNotNull(tempFileId);
        assertTrue(tempFileId.endsWith(".xlsx"));

        File storedFile = service.getTempFile(tempFileId);
        assertTrue(storedFile.exists());
    }

    @Test
    @DisplayName("getTempFile moet correct temp file pad teruggeven")
    void getTempFile_ShouldReturnFile() throws IOException {
        // Maak eerst even een nep-bestand aan zodat we zeker weten dat het bestaat
        Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "book-uploads");
        File dummyFile = new File(tempDir.toFile(), "abc.xlsx");
        dummyFile.createNewFile();

        File file = service.getTempFile("abc.xlsx");

        assertNotNull(file);
        assertTrue(file.getName().contains("abc.xlsx"));

        dummyFile.delete();
    }

    @Test
    @DisplayName("importBooks moet bestaand boek hergebruiken en enkel copy maken")
    void importBooks_ShouldReuseExistingBook_WhenBookExists() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "books.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                createValidWorkbookBytes());

        Book existingBook = new Book();
        existingBook.setIsbn("9781234567890");

        when(br.findByIsbnAndCampusId("9781234567890", 1L)).thenReturn(Optional.of(existingBook));

        service.importBooks(authentication, file);

        verify(br, never()).save(any(Book.class));
        ArgumentCaptor<BookCopy> captor = ArgumentCaptor.forClass(BookCopy.class);
        verify(bcr, times(1)).save(captor.capture());
        assertEquals(campus, captor.getValue().getCampus());
    }

    @Test
    @DisplayName("validateBooksStream moet een emitter teruggeven")
    void validateBooksStream_ShouldReturnEmitter() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "books.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                createValidWorkbookBytes());

        String tempFileId = service.storeTempFile(file);
        File tempFile = service.getTempFile(tempFileId);

        when(br.findAllIsbns()).thenReturn(List.of());

        SseEmitter emitter = service.validateBooksStream(tempFile);

        assertNotNull(emitter);
    }

    @Test
    @DisplayName("importBooks moet lege rijen negeren")
    void importBooks_ShouldSkipEmptyRows() throws Exception {
        // Maak een excel met een lege rij
        try (Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Books");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("ISBN");
            header.createCell(1).setCellValue("Title");

            // Rij 1 is expres helemaal leeg
            sheet.createRow(1);

            workbook.write(out);

            MockMultipartFile file = new MockMultipartFile(
                    "file", "books_empty.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    out.toByteArray());

            // Act
            service.importBooks(authentication, file);

            // Assert: Er mag helemaal niks worden opgeslagen
            verify(br, never()).save(any(Book.class));
            verify(bcr, never()).save(any(BookCopy.class));
        }
    }

    @Test
    @DisplayName("importBooks moet RuntimeException gooien als repository faalt")
    void importBooks_ShouldThrowException_WhenRepositoryFails() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "books.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                createValidWorkbookBytes());

        when(br.findByIsbnAndCampusId(anyString(), anyLong())).thenThrow(new IllegalArgumentException("Database disconnect"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> service.importBooks(authentication, file));
        assertTrue(exception.getMessage().contains("Gefailed om boeken te importeren"));
    }

    @Test
    @DisplayName("importBooks moet exception gooien bij een ongeldig (corrupt) excel bestand")
    void importBooks_ShouldThrowException_WhenFileIsCorrupt() {
        MockMultipartFile corruptFile = new MockMultipartFile(
                "file",
                "corrupt.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "dit is geen geldige excel data".getBytes());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> service.importBooks(authentication, corruptFile));
        assertTrue(exception.getMessage().contains("Gefailed om boeken te importeren"));
    }

    private byte[] createValidWorkbookBytes() throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Books");

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("ISBN");
            header.createCell(1).setCellValue("Title");
            header.createCell(2).setCellValue("Author");
            header.createCell(3).setCellValue("Pages");
            header.createCell(4).setCellValue("Description");
            header.createCell(5).setCellValue("ImageUrl");
            header.createCell(6).setCellValue("ReleaseYear");

            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("9781234567890");
            row.createCell(1).setCellValue("Testboek");
            row.createCell(2).setCellValue("Auteur");
            row.createCell(3).setCellValue(123);
            row.createCell(4).setCellValue("Beschrijving");
            row.createCell(5).setCellValue("https://example.com/image.jpg");
            row.createCell(6).setCellValue(2020);
            row.createCell(7).setCellValue("Fictie algemeen");
            row.createCell(8).setCellValue("Fantasy");
            row.createCell(9).setCellValue("");
            row.createCell(10).setCellValue("");
            row.createCell(11).setCellValue("A");

            workbook.write(out);
            return out.toByteArray();
        }
    }
}
