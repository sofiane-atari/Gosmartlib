package edu.ap.bib_backend.seed.dev;

import edu.ap.bib_backend.dto.BookDTO;
import edu.ap.bib_backend.dto.TagDTO;
import edu.ap.bib_backend.jpa.entity.BookCopy;
import edu.ap.bib_backend.jpa.repository.BookCopyRepository;
import edu.ap.bib_backend.jpa.repository.BookRepository;
import edu.ap.bib_backend.service.book.BookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class BookDevSeeder {

    private final BookService bookService;
    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;

    public BookDevSeeder(BookService bookService, BookRepository bookRepository,
                         BookCopyRepository bookCopyRepository) {
        this.bookService = bookService;
        this.bookRepository = bookRepository;
        this.bookCopyRepository = bookCopyRepository;
    }

    private static final List<BookData> BOOK_DATA = List.of(
            new BookData("9780135957059", "The Pragmatic Programmer", "Andrew Hunt & David Thomas", 352,
                    "De ultieme gids voor software-ontwikkelaars om vakmanschap en code-kwaliteit te verbeteren.",
                    "https://covers.openlibrary.org/b/isbn/9780135957059-L.jpg", 2019,
                    List.of("Software-architectuur")),
            new BookData("9780132350884", "Clean Code", "Robert C. Martin", 464,
                    "Een handboek voor het schrijven van leesbare, onderhoudbare en efficiënte software.",
                    "https://covers.openlibrary.org/b/isbn/9780132350884-L.jpg", 2008,
                    List.of("Java")),
            new BookData("9780596007126", "Head First Design Patterns", "Eric Freeman & Elisabeth Robson", 638,
                    "Een visuele en toegankelijke introductie tot herbruikbare software-oplossingen.",
                    "https://covers.openlibrary.org/b/isbn/9780596007126-L.jpg", 2004,
                    List.of("Software-architectuur", "Java")),
            new BookData("9780201616224", "Refactoring", "Martin Fowler", 448,
                    "Technieken om de interne structuur van bestaande code te verbeteren zonder het gedrag te wijzigen.",
                    "https://covers.openlibrary.org/b/isbn/9780201616224-L.jpg", 1999,
                    List.of("Software-architectuur")),
            new BookData("9780137081073", "The Clean Coder", "Robert C. Martin", 256,
                    "Een gids over professioneel gedrag, discipline en de ethiek van een softwareontwikkelaar.",
                    "https://covers.openlibrary.org/b/isbn/9780137081073-L.jpg", 2011,
                    List.of("Software-architectuur")),
            new BookData("9780321125217", "Domain-Driven Design", "Eric Evans", 560,
                    "Over het modelleren van complexe software op basis van de onderliggende business-logica.",
                    "https://covers.openlibrary.org/b/isbn/9780321125217-L.jpg", 2003,
                    List.of("Software-architectuur")),
            new BookData("9780262033848", "Introduction to Algorithms", "Cormen, Leiserson, Rivest & Stein", 1312,
                    "Het standaardwerk over algoritmes, datastructuren en computationele complexiteit.",
                    "https://covers.openlibrary.org/b/isbn/9780262033848-L.jpg", 2009,
                    List.of("Algoritmes")),
            new BookData("9780134685991", "Effective Java", "Joshua Bloch", 416,
                    "De beste praktijken voor het schrijven van robuuste en efficiënte Java-code.",
                    "https://covers.openlibrary.org/b/isbn/9780134685991-L.jpg", 2017,
                    List.of("Java")),
            new BookData("9780134494166", "Design Patterns", "Gamma, Helm, Johnson & Vlissides", 395,
                    "De klassieker die de 'Gang of Four' patronen introduceerde in software-architectuur.",
                    "https://covers.openlibrary.org/b/isbn/9780134494166-L.jpg", 1994,
                    List.of("Software-architectuur")),
            new BookData("9780131103627", "The C Programming Language", "Brian Kernighan & Dennis Ritchie", 272,
                    "Het gezaghebbende boek over de programmeertaal C, geschreven door de makers zelf.",
                    "https://covers.openlibrary.org/b/isbn/9780131103627-L.jpg", 1988,
                    List.of()),
            new BookData("9780201835953", "The Mythical Man-Month", "Frederick Brooks", 336,
                    "Essays over software-engineering en het beheer van complexe softwareprojecten.",
                    "https://covers.openlibrary.org/b/isbn/9780201835953-L.jpg", 1975,
                    List.of("Software-architectuur")),
            new BookData("9780735619678", "Code Complete", "Steve McConnell", 960,
                    "Een praktische gids over softwareconstructie en het bouwen van kwalitatieve code.",
                    "https://covers.openlibrary.org/b/isbn/9780735619678-L.jpg", 2004,
                    List.of("Software-architectuur")),
            new BookData("9780321356680", "Working Effectively with Legacy Code", "Michael Feathers", 456,
                    "Hoe je veilig wijzigingen aanbrengt in grote, ongeteste bestaande codebases.",
                    "https://covers.openlibrary.org/b/isbn/9780321356680-L.jpg", 2004,
                    List.of("Software-architectuur")),
            new BookData("9781617294532", "Grokking Algorithms", "Aditya Bhargava", 256,
                    "Een volledig geïllustreerde, gebruiksvriendelijke gids voor het begrijpen van algoritmes.",
                    "https://covers.openlibrary.org/b/isbn/9781617294532-L.jpg", 2016,
                    List.of("Algoritmes")),
            new BookData("9780596517748", "JavaScript: The Good Parts", "Douglas Crockford", 176,
                    "Een focus op de beste en krachtigste onderdelen van de programmeertaal JavaScript.",
                    "https://covers.openlibrary.org/b/isbn/9780596517748-L.jpg", 2008,
                    List.of("JavaScript")),
            new BookData("9781491950296", "Building Microservices", "Sam Newman", 280,
                    "Een overzicht van het ontwerpen en beheren van gedistribueerde systemen.",
                    "https://covers.openlibrary.org/b/isbn/9781491950296-L.jpg", 2015,
                    List.of("DevOps", "Software-architectuur")),
            new BookData("9781118831120", "Cracking the Coding Interview", "Gayle Laakmann McDowell", 687,
                    "189 programmeervragen en oplossingen om je voor te bereiden op technische interviews.",
                    "https://covers.openlibrary.org/b/isbn/9781118831120-L.jpg", 2015,
                    List.of("Algoritmes")),
            new BookData("9780131101630", "Structure and Interpretation of Computer Programs", "Abelson & Sussman", 657,
                    "De fundamentele concepten van programmeren via de taal Scheme.",
                    "https://covers.openlibrary.org/b/isbn/9780131101630-L.jpg", 1996,
                    List.of("Software-architectuur")),
            new BookData("9780321146533", "Test Driven Development: By Example", "Kent Beck", 240,
                    "Een introductie tot de methodiek waarbij je tests schrijft voordat je de code schrijft.",
                    "https://covers.openlibrary.org/b/isbn/9780321146533-L.jpg", 2002,
                    List.of("Software-architectuur")),
            new BookData("9781492045526", "Fluent Python", "Luciano Ramalho", 832,
                    "Leer hoe je effectieve en idiomatische Python-code schrijft door de unieke functies van de taal te benutten.",
                    "https://covers.openlibrary.org/b/isbn/9781492045526-L.jpg", 2022,
                    List.of("Python"))
    );

    public BookSeedResult seed(Authentication authentication, Long campusId, Map<String, Long> tagNameToId) {
        List<Long> createdBookIds = new ArrayList<>();
        int booksCreated = 0;
        int copiesCreated = 0;

        for (BookData data : BOOK_DATA) {
            if (bookRepository.findByIsbnAndCampusId(data.isbn, campusId).isPresent()) {
                log.info("Boek overgeslagen (bestaat al): {}", data.title);
                continue;
            }

            try {
                BookDTO dto = new BookDTO();
                dto.setIsbn(data.isbn);
                dto.setTitle(data.title);
                dto.setAuthor(data.author);
                dto.setPages(data.pages);
                dto.setDescription(data.description);
                dto.setImageUrl(data.imageUrl);
                dto.setReleaseYear(data.releaseYear);
                dto.setDidactic(false);
                dto.setCampusId(campusId);

                List<TagDTO> bookTags = new ArrayList<>();
                for (String tagName : data.tagNames) {
                    Long tagId = tagNameToId.get(tagName);
                    if (tagId != null) {
                        bookTags.add(new TagDTO(tagId, null, null, null, null));
                    }
                }
                dto.setTags(bookTags);

                BookDTO createdBook = bookService.addBook(dto, authentication);
                Long bookId = createdBook.getId();
                createdBookIds.add(bookId);
                booksCreated++;

                for (int i = 0; i < 2; i++) {
                    try {
                        bookService.addCopyToBook(bookId, campusId, authentication);
                        copiesCreated++;
                    } catch (Exception e) {
                        log.warn("Kon extra exemplaar niet toevoegen voor boek '{}': {}", data.title, e.getMessage());
                    }
                }

                log.info("Boek aangemaakt: {} (ISBN: {})", data.title, data.isbn);
            } catch (Exception e) {
                log.warn("Kon boek niet aanmaken '{}': {}", data.title, e.getMessage());
            }
        }

        log.info("Boek seeding voltooid: {} van {} boeken aangemaakt, {} extra exemplaren",
                booksCreated, BOOK_DATA.size(), copiesCreated);
        return new BookSeedResult(createdBookIds, booksCreated, copiesCreated);
    }

    public List<Long> getAvailableCopyIds(Long bookId, Long campusId) {
        return bookCopyRepository.findAvailableCopiesByBookIdAndCampusId(bookId, campusId)
                .stream()
                .map(BookCopy::getId)
                .toList();
    }

    public record BookData(String isbn, String title, String author, int pages,
                           String description, String imageUrl, int releaseYear,
                           List<String> tagNames) {}

    public record BookSeedResult(List<Long> bookIds, int booksCreated, int copiesCreated) {}
}