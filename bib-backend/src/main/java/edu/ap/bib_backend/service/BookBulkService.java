package edu.ap.bib_backend.service;

import edu.ap.bib_backend.dto.FileValidationError;
import edu.ap.bib_backend.dto.FileValidationResult;
import edu.ap.bib_backend.jpa.entity.*;
import edu.ap.bib_backend.jpa.repository.BookRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BookBulkService {
    private final BookRepository br;

    @Transactional
    public FileValidationResult validateBooks(MultipartFile file) {
        List<FileValidationError> errors = new ArrayList<>();
        Set<String> existingSet = new HashSet<>(br.findAllIsbns());
        Set<String> seenIsbns = new HashSet<>();
        int validRows = 0;

        try (InputStream is = file.getInputStream(); Workbook wb = WorkbookFactory.create(is)) {
            Sheet sheet = wb.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || row.getCell(0) == null)
                    continue;

                boolean rowValid = true;
                DataFormatter formatter = new DataFormatter();
                String isbn = formatter.formatCellValue(row.getCell(0)).trim();
                String title = formatter.formatCellValue(row.getCell(1)).trim();
                String author = formatter.formatCellValue(row.getCell(2)).trim();
                int pages = Integer.parseInt(formatter.formatCellValue(row.getCell(3)));
                String descirption = formatter.formatCellValue(row.getCell(4)).trim();
                String imageUrl = formatter.formatCellValue(row.getCell(5)).trim();
                int releaseYear = Integer.parseInt(formatter.formatCellValue(row.getCell(6)).trim());

                if (isbn.isEmpty()) {
                    errors.add(new FileValidationError(i + 1, "ISBN", "ISBN is leeg"));
                    rowValid = false;
                } else if (seenIsbns.contains(isbn)) {
                    errors.add(new FileValidationError(i + 1, "ISBN", "Duplicaat ISBN in geüploade bestand"));
                    rowValid = false;
                } else if (existingSet.contains(isbn)) {
                    errors.add(new FileValidationError(i + 1, "ISBN", "Duplicaat ISBN in de database"));
                    rowValid = false;
                } else {
                    seenIsbns.add(isbn);
                }
                if (title.isEmpty()) {
                    errors.add(new FileValidationError(i + 1, "Title", "Title is empty"));
                    rowValid = false;
                }

                if (author.isEmpty()) {
                    errors.add(new FileValidationError(i + 1, "Author", "Author is empty"));
                    rowValid = false;
                }

                if (pages == 0) {
                    errors.add(new FileValidationError(i + 1, "pages", "Pages is empty"));
                    rowValid = false;
                }

                if (descirption.isEmpty()) {
                    errors.add(new FileValidationError(i + 1, "descirption", "Descirption is empty"));
                    rowValid = false;
                }

                if (imageUrl.isEmpty()) {
                    errors.add(new FileValidationError(i + 1, "imageUrl", "Image url is empty"));
                    rowValid = false;
                }
                if (releaseYear == 0) {
                    errors.add(new FileValidationError(i + 1, "releaseYear", "Release year is empty"));
                    rowValid = false;
                }

                if (rowValid)
                    validRows++;
            }

            int totalRows = sheet.getLastRowNum();
            return new FileValidationResult(totalRows, validRows, errors.size(), errors);

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
            throw new RuntimeException("Failed to import books: " + e.getMessage());
        }
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
