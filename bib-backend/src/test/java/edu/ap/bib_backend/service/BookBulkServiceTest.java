package edu.ap.bib_backend.service;

import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.BookCopy;
import edu.ap.bib_backend.jpa.repository.BookCopyRepository;
import edu.ap.bib_backend.jpa.repository.BookRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BookBulkServiceTest {

    private BookRepository br;
    private BookCopyRepository bcr;
    private BookBulkService service;

    @BeforeEach
    void setUp() {
        br = mock(BookRepository.class);
        bcr = mock(BookCopyRepository.class);
        service = new BookBulkService(br, bcr);
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
    void getTempFile_ShouldReturnFile() {
        File file = service.getTempFile("abc.xlsx");

        assertNotNull(file);
        assertTrue(file.getName().contains("abc.xlsx"));
    }

    @Test
    @DisplayName("importBooks moet nieuw boek opslaan en een copy maken")
    void importBooks_ShouldSaveBookAndCopy_WhenBookDoesNotExist() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "books.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                createValidWorkbookBytes());

        when(br.findByISBN("9781234567890")).thenReturn(null);
        when(br.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.importBooks(file);

        verify(br, times(1)).save(any(Book.class));
        verify(bcr, times(1)).save(any(BookCopy.class));
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
        existingBook.setISBN("9781234567890");

        when(br.findByISBN("9781234567890")).thenReturn(existingBook);

        service.importBooks(file);

        verify(br, never()).save(any(Book.class));
        verify(bcr, times(1)).save(any(BookCopy.class));
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
            row.createCell(1).setCellValue("Test Book");
            row.createCell(2).setCellValue("Test Author");
            row.createCell(3).setCellValue("250");
            row.createCell(4).setCellValue("Beschrijving");
            row.createCell(5).setCellValue("https://example.com/image.jpg");
            row.createCell(6).setCellValue("2020");

            workbook.write(out);
            return out.toByteArray();
        }
    }
}