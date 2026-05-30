package edu.ap.bib_backend.service.book;

import edu.ap.bib_backend.dto.FileValidationError;
import edu.ap.bib_backend.dto.ValidationProgress;
import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.BookCopy;
import edu.ap.bib_backend.jpa.entity.Campus;
import edu.ap.bib_backend.jpa.entity.Genre;
import edu.ap.bib_backend.jpa.entity.ReadingLevel;

import edu.ap.bib_backend.jpa.repository.BookCopyRepository;
import edu.ap.bib_backend.jpa.repository.BookRepository;
import edu.ap.bib_backend.jpa.repository.CampusRepository;
import edu.ap.bib_backend.jpa.repository.GenreRepository;
import edu.ap.bib_backend.jpa.repository.ReadingLevelRepository;
import edu.ap.bib_backend.service.user.CurrentUserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.*;
import java.time.Year;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BookBulkService {

    private final BookRepository br;
    private final BookCopyRepository bcr;
    private final GenreRepository genreRepository;
    private final ReadingLevelRepository readingLevelRepository;
    private final CampusRepository campusRepository;
    private final CurrentUserService currentUserService;

    private final Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "book-uploads");

    public String storeTempFile(MultipartFile file) throws IOException {
        Files.createDirectories(tempDir);

        String extension = getSafeExtension(file.getOriginalFilename());
        String tempFileId = UUID.randomUUID() + extension;
        Path target = tempDir.resolve(tempFileId);

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        }

        return tempFileId;
    }

    public File getTempFile(String tempFileId) {
        return tempDir.resolve(tempFileId).toFile();
    }

    private String getSafeExtension(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return ".tmp";
        }

        String safeName = Paths.get(originalFilename).getFileName().toString();
        int lastDot = safeName.lastIndexOf('.');

        if (lastDot < 0 || lastDot == safeName.length() - 1) {
            return ".tmp";
        }

        return safeName.substring(lastDot).toLowerCase();
    }

    @Transactional
    public void importBooks(Authentication authentication, MultipartFile file, Long campusId) {
        Campus campus = resolveCampusForCurrentUserSchool(authentication, campusId);

        try (InputStream is = file.getInputStream(); Workbook wb = WorkbookFactory.create(is)) {
            Sheet sheet = wb.getSheetAt(0);

            Map<String, Book> importedBooksByIsbn = new HashMap<>();
            Map<String, Book> importedBooksByNaturalKey = new HashMap<>();

            DataFormatter formatter = new DataFormatter();
            FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row rw = sheet.getRow(i);

                if (rw == null || isRowEmpty(rw, formatter, evaluator)) {
                    continue;
                }

                String isbn = formatter.formatCellValue(rw.getCell(0), evaluator).trim();
                String title = formatter.formatCellValue(rw.getCell(1), evaluator).trim();
                String author = formatter.formatCellValue(rw.getCell(2), evaluator).trim();
                String yearStr = formatter.formatCellValue(rw.getCell(6), evaluator).trim();

                Integer releaseYear = null;
                if (!yearStr.isEmpty()) {
                    releaseYear = Integer.parseInt(yearStr);
                }

                String naturalKey = buildNaturalKey(title, author, releaseYear);

                Book book = null;

                if (!isbn.isEmpty() && importedBooksByIsbn.containsKey(isbn)) {
                    book = importedBooksByIsbn.get(isbn);
                }

                if (book == null && importedBooksByNaturalKey.containsKey(naturalKey)) {
                    book = importedBooksByNaturalKey.get(naturalKey);
                }

                if (book == null && !isbn.isEmpty()) {
                    book = br.findByIsbnAndCampusId(isbn, campus.getId()).orElse(null);
                }

                if (book == null) {
                    book = br.findByTitleIgnoreCaseAndAuthorIgnoreCaseAndReleaseYear(title, author, releaseYear)
                            .stream()
                            .filter(existingBook -> existingBook.getCampus() != null
                                    && existingBook.getCampus().getId().equals(campus.getId()))
                            .findFirst()
                            .orElse(null);
                }

                if (book == null) {
                    book = mapRowToBook(rw, evaluator, campus);
                    book = br.save(book);
                }

                if (!isbn.isEmpty()) {
                    importedBooksByIsbn.put(isbn, book);
                }

                importedBooksByNaturalKey.put(naturalKey, book);

                BookCopy copy = new BookCopy();
                copy.setBook(book);
                copy.setCampus(campus);
                bcr.save(copy);
            }
        } catch (Exception e) {
            throw new RuntimeException("Gefailed om boeken te importeren: " + e.getMessage(), e);
        }
    }

    private Campus resolveCampusForCurrentUserSchool(Authentication authentication, Long campusId) {
        if (campusId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "campusId is verplicht.");
        }

        Long schoolId = currentUserService.getCurrentUserSchool(authentication).getId();
        return campusRepository.findByIdAndSchoolId(campusId, schoolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campus niet gevonden."));
    }

    public SseEmitter validateBooksStream(File file) {
        SseEmitter emitter = new SseEmitter(0L);

        new Thread(() -> {
            try (InputStream is = new FileInputStream(file); Workbook wb = WorkbookFactory.create(is)) {
                Sheet sheet = wb.getSheetAt(0);
                int totalRows = sheet.getLastRowNum();
                DataFormatter formatter = new DataFormatter();
                FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();

                Set<String> existingIsbns = new HashSet<>(br.findAllIsbns());
                Set<String> seenIsbns = new HashSet<>();
                Set<String> seenNaturalKeys = new HashSet<>();

                for (int i = 1; i <= totalRows; i++) {
                    Row row = sheet.getRow(i);
                    if (row == null || isRowEmpty(row, formatter, evaluator)) {
                        continue;
                    }

                    List<FileValidationError> rowErrors = validateRow(
                            row,
                            i,
                            formatter,
                            evaluator,
                            existingIsbns,
                            seenIsbns,
                            seenNaturalKeys
                    );

                    int progress = (int) (((double) i / totalRows) * 100);

                    ValidationProgress update = new ValidationProgress(
                            i,
                            totalRows,
                            progress,
                            rowErrors
                    );

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

    private boolean isRowEmpty(Row row, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (row == null) {
            return true;
        }

        for (int i = 0; i <= 11; i++) {
            Cell cell = row.getCell(i);
            String value = formatter.formatCellValue(cell, evaluator).trim();
            if (!value.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    private List<FileValidationError> validateRow(
            Row row,
            int rowIndex,
            DataFormatter formatter,
            FormulaEvaluator evaluator,
            Set<String> existingIsbns,
            Set<String> seenIsbns,
            Set<String> seenNaturalKeys
    ) {
        List<FileValidationError> rowErrors = new ArrayList<>();
        int rowNumber = rowIndex + 1;

        String isbn = formatter.formatCellValue(row.getCell(0), evaluator).trim();
        String title = formatter.formatCellValue(row.getCell(1), evaluator).trim();
        String author = formatter.formatCellValue(row.getCell(2), evaluator).trim();
        String pagesStr = formatter.formatCellValue(row.getCell(3), evaluator).trim();
        String imageUrl = formatter.formatCellValue(row.getCell(5), evaluator).trim();
        String yearStr = formatter.formatCellValue(row.getCell(6), evaluator).trim();
        String mainGenre = formatter.formatCellValue(row.getCell(7), evaluator).trim();
        String subGenre1 = formatter.formatCellValue(row.getCell(8), evaluator).trim();
        String subGenre2 = formatter.formatCellValue(row.getCell(9), evaluator).trim();
        String subGenre3 = formatter.formatCellValue(row.getCell(10), evaluator).trim();
        String readingLevel = formatter.formatCellValue(row.getCell(11), evaluator).trim();

        boolean hasIsbnMatch = false;

        if (!isbn.isEmpty()) {
            if (!isbn.matches("\\d{13}")) {
                rowErrors.add(new FileValidationError(rowNumber, "ISBN", "ISBN moet 13 cijfers bevatten", "error"));
            } else {
                if (seenIsbns.contains(isbn)) {
                    rowErrors.add(new FileValidationError(
                            rowNumber,
                            "ISBN",
                            "ISBN komt dubbel voor in het bestand. Er zal een exemplaar toegevoegd worden met de gegevens van de eerst voorkomende rij",
                            "warning"
                    ));
                    hasIsbnMatch = true;
                } else if (existingIsbns.contains(isbn)) {
                    rowErrors.add(new FileValidationError(
                            rowNumber,
                            "ISBN",
                            "ISBN bestaat al in de database. Er zal een exemplaar toegevoegd worden met de gegevens in de database",
                            "warning"
                    ));
                    hasIsbnMatch = true;
                }

                seenIsbns.add(isbn);
            }
        }

        if (title.isEmpty()) {
            rowErrors.add(new FileValidationError(rowNumber, "Title", "Titel is leeg"));
        }

        if (author.isEmpty()) {
            rowErrors.add(new FileValidationError(rowNumber, "Author", "Auteur is leeg"));
        }

        if (pagesStr.isEmpty()) {
            rowErrors.add(new FileValidationError(rowNumber, "Pages", "Pagina's is leeg"));
        } else {
            try {
                int pages = Integer.parseInt(pagesStr);
                if (pages <= 0) {
                    rowErrors.add(new FileValidationError(rowNumber, "Pages", "Pagina's moeten meer dan 0 zijn"));
                }
            } catch (NumberFormatException e) {
                rowErrors.add(new FileValidationError(rowNumber, "Pages", "Pagina's moet een nummer zijn"));
            }
        }

        Integer releaseYear = null;
        if (!yearStr.isEmpty()) {
            try {
                releaseYear = Integer.parseInt(yearStr);
            } catch (NumberFormatException ignored) {
            }
        }

        if (!hasIsbnMatch && !title.isEmpty() && !author.isEmpty() && releaseYear != null) {
            String naturalKey = buildNaturalKey(title, author, releaseYear);

            if (seenNaturalKeys.contains(naturalKey)) {
                rowErrors.add(new FileValidationError(
                        rowNumber,
                        "Title/Author/ReleaseYear",
                        "De combinatie titel + auteur + jaar komt dubbel voor in het bestand. Er zal een exemplaar toegevoegd worden met de gegevens van de eerst voorkomende rij.",
                        "warning"
                ));
            } else if (!br.findByTitleIgnoreCaseAndAuthorIgnoreCaseAndReleaseYear(title, author, releaseYear).isEmpty()) {
                rowErrors.add(new FileValidationError(
                        rowNumber,
                        "Title/Author/ReleaseYear",
                        "De combinatie titel + auteur + jaar bestaat al in de database. Er zal een exemplaar toegevoegd worden met de bestaande boekgegevens.",
                        "warning"
                ));
            }

            seenNaturalKeys.add(naturalKey);
        }

        if (!imageUrl.isEmpty()) {
            try {
                new URL(imageUrl);
            } catch (MalformedURLException e) {
                rowErrors.add(new FileValidationError(rowNumber, "ImageUrl", "Invalide URL formaat"));
            }
        }

        if (yearStr.isEmpty()) {
            rowErrors.add(new FileValidationError(rowNumber, "ReleaseYear", "Uitgave jaar is leeg"));
        } else {
            try {
                int year = Integer.parseInt(yearStr);
                int currentYear = Year.now().getValue();

                if (year < 1000 || year > currentYear) {
                    rowErrors.add(new FileValidationError(rowNumber, "ReleaseYear", "Invalide jaar"));
                }
            } catch (NumberFormatException e) {
                rowErrors.add(new FileValidationError(rowNumber, "ReleaseYear", "Ongeldig jaartal: " + e.getMessage()));
            }
        }

        boolean hasAnyGenre =
                !mainGenre.isEmpty()
                        || !subGenre1.isEmpty()
                        || !subGenre2.isEmpty()
                        || !subGenre3.isEmpty();

        if (hasAnyGenre) {
            if (mainGenre.isEmpty()) {
                rowErrors.add(new FileValidationError(rowNumber, "Hoofdgenre", "Hoofdgenre is verplicht als je subgenres invult"));
            } else {
                Genre mainGenreEntity = genreRepository.findByName(mainGenre).orElse(null);
                if (mainGenreEntity == null) {
                    rowErrors.add(new FileValidationError(rowNumber, "Hoofdgenre", "Hoofdgenre bestaat niet"));
                } else if (mainGenreEntity.getParentGenre() != null) {
                    rowErrors.add(new FileValidationError(rowNumber, "Hoofdgenre",
                            "Hoofdgenre moet een hoofdgenre zijn, geen subgenre"));
                }

                List<String> subGenres = new ArrayList<>();
                if (!subGenre1.isEmpty()) subGenres.add(subGenre1);
                if (!subGenre2.isEmpty()) subGenres.add(subGenre2);
                if (!subGenre3.isEmpty()) subGenres.add(subGenre3);

                List<String> uniqueSubGenres = new ArrayList<>(new LinkedHashSet<>(subGenres));
                Genre mainGenreEntityWithParent = genreRepository.findByNameWithParent(mainGenre).orElse(null);

                for (String subGenreName : uniqueSubGenres) {
                    Genre subGenreEntity = genreRepository.findByNameWithParent(subGenreName).orElse(null);

                    if (subGenreEntity == null) {
                        rowErrors.add(new FileValidationError(rowNumber, "Subgenres", "Subgenre bestaat niet: " + subGenreName));
                    } else if (subGenreEntity.getParentGenre() == null) {
                        rowErrors.add(new FileValidationError(rowNumber, "Subgenres",
                                "Subgenre is geen echt subgenre: " + subGenreName));
                    } else if (mainGenreEntityWithParent != null
                            && subGenreEntity.getParentGenre() != null
                            && !subGenreEntity.getParentGenre().getId().equals(mainGenreEntityWithParent.getId())) {
                        rowErrors.add(new FileValidationError(rowNumber, "Subgenres",
                                "Subgenre '" + subGenreName + "' hoort niet bij hoofdgenre '" + mainGenre + "'"));
                    }
                }
            }
        }

        if (!readingLevel.isEmpty()) {
            ReadingLevel levelEntity = readingLevelRepository.findByNiveauName(readingLevel);
            if (levelEntity == null) {
                rowErrors.add(new FileValidationError(rowNumber, "ReadingLevel", "Leesniveau bestaat niet"));
            }
        }

        return rowErrors;
    }

    private Book mapRowToBook(Row row, FormulaEvaluator evaluator, Campus campus) {
        Book book = new Book();
        DataFormatter formatter = new DataFormatter();

        String isbn = formatter.formatCellValue(row.getCell(0), evaluator).trim();
        String title = formatter.formatCellValue(row.getCell(1), evaluator).trim();
        String author = formatter.formatCellValue(row.getCell(2), evaluator).trim();
        String pages = formatter.formatCellValue(row.getCell(3), evaluator).trim();
        String description = formatter.formatCellValue(row.getCell(4), evaluator).trim();
        String imageUrl = formatter.formatCellValue(row.getCell(5), evaluator).trim();
        String releaseYear = formatter.formatCellValue(row.getCell(6), evaluator).trim();
        String subGenre1 = formatter.formatCellValue(row.getCell(8), evaluator).trim();
        String subGenre2 = formatter.formatCellValue(row.getCell(9), evaluator).trim();
        String subGenre3 = formatter.formatCellValue(row.getCell(10), evaluator).trim();
        String readingLevelName = formatter.formatCellValue(row.getCell(11), evaluator).trim();

        book.setCampus(campus);
        book.setIsbn(isbn.isEmpty() ? null : isbn);
        book.setTitle(title);
        book.setAuthor(author);
        book.setPages(Integer.parseInt(pages));
        book.setDescription(description.isBlank() ? null : description);
        book.setImageUrl(imageUrl.isBlank() ? null : imageUrl);
        book.setReleaseYear(Integer.parseInt(releaseYear));

        Set<String> uniqueSubGenreNames = new LinkedHashSet<>();

        if (!subGenre1.isBlank()) uniqueSubGenreNames.add(subGenre1);
        if (!subGenre2.isBlank()) uniqueSubGenreNames.add(subGenre2);
        if (!subGenre3.isBlank()) uniqueSubGenreNames.add(subGenre3);

        Set<Genre> selectedGenres = new HashSet<>();

        for (String subGenreName : uniqueSubGenreNames) {
            genreRepository.findByName(subGenreName).ifPresent(selectedGenres::add);
        }

        book.setGenres(selectedGenres);

        if (!readingLevelName.isBlank()) {
            ReadingLevel level = readingLevelRepository.findByNiveauName(readingLevelName);
            book.setReadingLevel(level);
        } else {
            book.setReadingLevel(null);
        }

        return book;
    }

    private String buildNaturalKey(String title, String author, Integer releaseYear) {
        return normalize(title) + "|" + normalize(author) + "|" + (releaseYear == null ? "" : releaseYear);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
