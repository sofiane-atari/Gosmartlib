package edu.ap.bib_backend.service;

import edu.ap.bib_backend.dto.FileValidationError;
import edu.ap.bib_backend.dto.FileValidationResult;
import edu.ap.bib_backend.dto.ValidationProgress;
import edu.ap.bib_backend.jpa.entity.*;
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
import java.util.UUID;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BookBulkService {
    private final BookRepository br;
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
    public FileValidationResult validateBooks(MultipartFile file) {

        List<FileValidationError> errors = new ArrayList<>();

        Set<String> existingIsbns = new HashSet<>(br.findAllIsbns());
        Set<String> seenIsbns = new HashSet<>();

        int validRows = 0;

        try (
                InputStream is = file.getInputStream();
                Workbook wb = WorkbookFactory.create(is)) {

            Sheet sheet = wb.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {

                Row row = sheet.getRow(i);
                if (row == null)
                    continue;

                List<FileValidationError> rowErrors = validateRow(row, i, formatter, existingIsbns, seenIsbns);

                if (rowErrors.isEmpty()) {
                    validRows++;
                } else {
                    errors.addAll(rowErrors);
                }
            }

            int totalRows = sheet.getLastRowNum();

            return new FileValidationResult(
                    totalRows,
                    validRows,
                    errors.size(),
                    errors);

        } catch (Exception e) {
            throw new RuntimeException("Failed to validate books: " + e.getMessage());
        }
    }

    @Transactional
    public void importBooks(MultipartFile file) {
        try (InputStream is = file.getInputStream(); Workbook wb = WorkbookFactory.create(is)) {
            Sheet sheet = wb.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row rw = sheet.getRow(i);

                if (rw == null)
                    continue;
                if (rw.getCell(1) == null)
                    continue;

                Book book = mapRowToBook(rw);
                br.save(book);

            }
        } catch (Exception e) {
            throw new RuntimeException("gefailed om boeken te importeren" + e.getMessage());
        }
    }

    public SseEmitter validateBooksStream(File file) {
        SseEmitter emitter = new SseEmitter();

        new Thread(() -> {
            try (InputStream is = new FileInputStream(file); Workbook wb = WorkbookFactory.create(is)) {

                Sheet sheet = wb.getSheetAt(0);
                int totalRows = sheet.getLastRowNum();
                DataFormatter formatter = new DataFormatter();

                Set<String> existingIsbns = new HashSet<>(br.findAllIsbns());
                Set<String> seenIsbns = new HashSet<>();

                for (int i = 1; i <= totalRows; i++) {
                    Row row = sheet.getRow(i);
                    if (row == null)
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

        if (isbn.isEmpty())
            rowErrors.add(new FileValidationError(rowNumber, "ISBN", "ISBN is empty"));
        else if (existingIsbns.contains(isbn))
            rowErrors.add(new FileValidationError(rowNumber, "ISBN", "Duplicate ISBN in database"));
        else if (seenIsbns.contains(isbn))
            rowErrors.add(new FileValidationError(rowNumber, "ISBN", "Duplicate ISBN in uploaded file"));
        else
            seenIsbns.add(isbn);

        if (title.isEmpty())
            rowErrors.add(new FileValidationError(rowNumber, "Title", "Title is empty"));
        if (author.isEmpty())
            rowErrors.add(new FileValidationError(rowNumber, "Author", "Author is empty"));
        if (pagesStr.isEmpty())
            rowErrors.add(new FileValidationError(rowNumber, "Pages", "Pages is empty"));
        if (description.isEmpty())
            rowErrors.add(new FileValidationError(rowNumber, "Description", "Description is empty"));
        if (imageUrl.isEmpty())
            rowErrors.add(new FileValidationError(rowNumber, "ImageUrl", "Image URL is empty"));
        if (yearStr.isEmpty())
            rowErrors.add(new FileValidationError(rowNumber, "ReleaseYear", "Release year is empty"));

        return rowErrors;
    }

    private Book mapRowToBook(Row row) {
        Book book = new Book();
        DataFormatter formatter = new DataFormatter();

        book.setISBN(formatter.formatCellValue(row.getCell(0)));
        book.setTitle(formatter.formatCellValue(row.getCell(1)));
        book.setAuthor(formatter.formatCellValue(row.getCell(2)));
        book.setPages(Integer.parseInt(formatter.formatCellValue(row.getCell(3))));
        book.setDescription(formatter.formatCellValue(row.getCell(4)));
        book.setImageUrl(formatter.formatCellValue(row.getCell(5)));
        book.setReleaseYear(Integer.parseInt(formatter.formatCellValue(row.getCell(6))));

        return book;
    }

}
