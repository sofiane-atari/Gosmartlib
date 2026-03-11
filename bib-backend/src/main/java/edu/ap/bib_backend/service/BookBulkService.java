package edu.ap.bib_backend.service;

import edu.ap.bib_backend.jpa.entity.*;
import edu.ap.bib_backend.jpa.repository.BookRepository;

import lombok.RequiredArgsConstructor;

import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class BookBulkService {
    private final BookRepository br;

    public void importBooks(MultipartFile file) {
        try (InputStream is = file.getInputStream(); Workbook wb = WorkbookFactory.create(is)) {
            Sheet sheet = wb.getSheetAt(0);

            for (int i = 1; i < sheet.getLastRowNum(); i++) {
                Row rw = sheet.getRow(i);

                if (rw == null)
                    continue;

                Book book = new Book();

            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to import books: " + e.getMessage());
        }
    }

    private Book mapRowToBook(Row row) {
        Book book = new Book();

        book.setISBN(row.getCell(0).getStringCellValue());
        book.setTitle(row.getCell(1).getStringCellValue());
        book.setAuthor(row.getCell(2).getStringCellValue());
        book.setPages((int) row.getCell(3).getNumericCellValue());
        book.setDescription(row.getCell(4).getStringCellValue());
        book.setImageUrl(row.getCell(5).getStringCellValue());
        book.setReleaseYear((int) row.getCell(6).getNumericCellValue());

        String availability = row.getCell(7).getStringCellValue();
        book.setAvailability(Availability.valueOf(availability));

        book.setCopies((int) row.getCell(8).getNumericCellValue());

        return book;
    }

}
