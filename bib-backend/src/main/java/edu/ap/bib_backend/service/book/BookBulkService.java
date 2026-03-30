package edu.ap.bib_backend.service.book;

import edu.ap.bib_backend.dto.FileValidationError;
import edu.ap.bib_backend.dto.ValidationProgress;
import edu.ap.bib_backend.jpa.entity.*;
import edu.ap.bib_backend.jpa.enums.Availability;
import edu.ap.bib_backend.jpa.repository.BookCopyRepository;
import edu.ap.bib_backend.jpa.repository.BookRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Year;
import java.util.UUID;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BookBulkService {
    private final BookRepository br;
    private final BookCopyRepository bcr;
    private final Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "book-uploads");

    // Save uploaded file temporarily and return a temp file ID
    public String storeTempFile(MultipartFile file) throws IOException {
        String tempFileId = UUID.randomUUID().toString() + ".xlsx";
        Path target = tempDir.resolve(tempFileId);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        return tempFileId;
    }

    public File getTempFile(String tempFileId) {
        return tempDir.resolve(tempFileId).toFile();
    }

    @Transactional
    public void importBooks(MultipartFile file) {
        try (InputStream is = file.getInputStream(); Workbook wb = WorkbookFactory.create(is)) {
            Sheet sheet = wb.getSheetAt(0);
            Map<String, Book> importedBooks = new HashMap<>();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row rw = sheet.getRow(i);

                if (rw == null)
                    continue;
                if (rw.getCell(1) == null)
                    continue;

                DataFormatter formatter = new DataFormatter();
                String isbn = formatter.formatCellValue(rw.getCell(0)).trim();
                Book book;

                // check eerst of boek al in deze import zit
                if (importedBooks.containsKey(isbn)) {
                    book = importedBooks.get(isbn);
                } else {
                    Book existingBook = br.findByIsbn(isbn);

                    if (existingBook != null) {

                        book = existingBook;

                    } else {
                        book = mapRowToBook(rw);
                        book = br.save(book);
                        importedBooks.put(isbn, book);
                    }

                }
                // altijd een BookCopy maken
                BookCopy copy = new BookCopy();
                copy.setBook(book);
                copy.setAvailability(Availability.AVAILABLE);
                bcr.save(copy);

            }
        } catch (Exception e) {
            throw new RuntimeException("gefailed om boeken te importeren" + e.getMessage());
        }
    }

    private boolean isRowEmpty(Row row, DataFormatter formatter) {
        if (row == null)
            return true;

        for (int i = 0; i <= 6; i++) { // jouw 7 kolommen
            Cell cell = row.getCell(i);
            String value = formatter.formatCellValue(cell).trim();
            if (!value.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public SseEmitter validateBooksStream(File file) {
        SseEmitter emitter = new SseEmitter(0L);

        new Thread(() -> {
            try (InputStream is = new FileInputStream(file); Workbook wb = WorkbookFactory.create(is)) {

                Sheet sheet = wb.getSheetAt(0);
                int totalRows = sheet.getLastRowNum();
                DataFormatter formatter = new DataFormatter();

                Set<String> existingIsbns = new HashSet<>(br.findAllIsbns());
                Set<String> seenIsbns = new HashSet<>();

                for (int i = 1; i <= totalRows; i++) {
                    Row row = sheet.getRow(i);
                    if (row == null || isRowEmpty(row, formatter))
                        continue;

                    List<FileValidationError> rowErrors = validateRow(row, i, formatter, existingIsbns, seenIsbns);
                    int progress = (int) (((double) i / totalRows) * 100);

                    ValidationProgress update = new ValidationProgress(
                            i,
                            totalRows,
                            progress,
                            rowErrors);

                    emitter.send(SseEmitter.event().name("progress").data(update));
                    Thread.sleep(40);
                }

                emitter.send(SseEmitter.event().name("complete").data("done"));
                emitter.complete();

            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        }).start();

        return emitter;
    }

    private List<FileValidationError> validateRow(Row row, int rowIndex, DataFormatter formatter,
            Set<String> existingIsbns, Set<String> seenIsbns) {

        List<FileValidationError> rowErrors = new ArrayList<>();
        int rowNumber = rowIndex + 1;

        String isbn = formatter.formatCellValue(row.getCell(0)).trim();
        String title = formatter.formatCellValue(row.getCell(1)).trim();
        String author = formatter.formatCellValue(row.getCell(2)).trim();
        String pagesStr = formatter.formatCellValue(row.getCell(3)).trim();
        String description = formatter.formatCellValue(row.getCell(4)).trim();
        String imageUrl = formatter.formatCellValue(row.getCell(5)).trim();
        String yearStr = formatter.formatCellValue(row.getCell(6)).trim();

        // ISBN
        if (isbn.isEmpty()) {
            rowErrors.add(new FileValidationError(rowNumber, "ISBN", "ISBN is leeg"));
        } else if (!isbn.matches("\\d{13}")) {
            rowErrors.add(new FileValidationError(rowNumber, "ISBN", "ISBN moet 13 cijfers bevatten"));
        } else {
            seenIsbns.add(isbn);
        }

        // Title
        if (title.isEmpty())
            rowErrors.add(new FileValidationError(rowNumber, "Title", "Titel is leeg"));

        // Author
        if (author.isEmpty())
            rowErrors.add(new FileValidationError(rowNumber, "Author", "Auteur is leeg"));

        // Pages
        if (pagesStr.isEmpty()) {
            rowErrors.add(new FileValidationError(rowNumber, "Pages", "Pagina's is leeg"));
        } else {
            try {
                int pages = Integer.parseInt(pagesStr);
                if (pages <= 0)
                    rowErrors.add(new FileValidationError(rowNumber, "Pages", "Pagina's moeten meer dan 0 zijn"));
            } catch (NumberFormatException e) {
                rowErrors.add(new FileValidationError(rowNumber, "Pages", "Pagina's moet een nummer zijn"));
            }
        }

        // Description
        if (description.isEmpty())
            rowErrors.add(new FileValidationError(rowNumber, "Description", "Descriptie is leeg"));

        // Image URL
        if (imageUrl.isEmpty()) {
            rowErrors.add(new FileValidationError(rowNumber, "ImageUrl", "Image URL is leeg"));
        } else {
            try {
                new URL(imageUrl);
            } catch (MalformedURLException e) {
                rowErrors.add(new FileValidationError(rowNumber, "ImageUrl", "Invalide URL formaat"));
            }
        }

        // Release Year
        if (yearStr.isEmpty()) {
            rowErrors.add(new FileValidationError(rowNumber, "ReleaseYear", "Uitgave jaar is leeg"));
        } else {
            try {
                int year = Integer.parseInt(yearStr);
                int currentYear = Year.now().getValue();

                if (year < 1000 || year > currentYear)
                    rowErrors.add(new FileValidationError(rowNumber, "ReleaseYear", "Invalide jaar"));
            } catch (NumberFormatException e) {
                rowErrors.add(new FileValidationError(rowNumber, "ReleaseYear", "Uitgave jaar moet een nummer zijn"));
            }
        }

        return rowErrors;
    }

    private Book mapRowToBook(Row row) {
        Book book = new Book();
        DataFormatter formatter = new DataFormatter();

        book.setIsbn(formatter.formatCellValue(row.getCell(0)));
        book.setTitle(formatter.formatCellValue(row.getCell(1)));
        book.setAuthor(formatter.formatCellValue(row.getCell(2)));
        book.setPages(Integer.parseInt(formatter.formatCellValue(row.getCell(3))));
        book.setDescription(formatter.formatCellValue(row.getCell(4)));
        book.setImageUrl(formatter.formatCellValue(row.getCell(5)));
        book.setReleaseYear(Integer.parseInt(formatter.formatCellValue(row.getCell(6))));

        return book;
    }

}
