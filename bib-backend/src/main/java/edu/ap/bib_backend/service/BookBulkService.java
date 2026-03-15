package edu.ap.bib_backend.service;

import edu.ap.bib_backend.jpa.entity.*;
import edu.ap.bib_backend.jpa.repository.BookRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class BookBulkService {
    private final BookRepository br;

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
