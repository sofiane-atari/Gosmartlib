package edu.ap.bib_backend.seed;

import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.Campus;
import edu.ap.bib_backend.jpa.repository.BookRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookSeeder {

    private final BookRepository bookRepository;

    public BookSeeder(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public void seedBooksForCampusIfEmpty(Campus campus) {
        if (bookRepository.count() > 0) {
            System.out.println("BookSeeder overgeslagen: boeken bestaan al.");
            return;
        }

        bookRepository.saveAll(List.of(
                createBook(
                        "9780135957059",
                        "The Pragmatic Programmer",
                        "Andrew Hunt & David Thomas",
                        352,
                        "De ultieme gids voor software-ontwikkelaars om vakmanschap en code-kwaliteit te verbeteren.",
                        "https://covers.openlibrary.org/b/isbn/9780135957059-L.jpg",
                        2019,
                        campus),
                createBook(
                        "9780132350884",
                        "Clean Code",
                        "Robert C. Martin",
                        464,
                        "Een handboek voor het schrijven van leesbare, onderhoudbare en efficiënte software.",
                        "https://covers.openlibrary.org/b/isbn/9780132350884-L.jpg",
                        2008,
                        campus),
                createBook(
                        "9780596007126",
                        "Head First Design Patterns",
                        "Eric Freeman & Elisabeth Robson",
                        638,
                        "Een visuele en toegankelijke introductie tot herbruikbare software-oplossingen.",
                        "https://covers.openlibrary.org/b/isbn/9780596007126-L.jpg",
                        2004,
                        campus),
                createBook(
                        "9780201616224",
                        "Refactoring",
                        "Martin Fowler",
                        448,
                        "Technieken om de interne structuur van bestaande code te verbeteren zonder het gedrag te wijzigen.",
                        "https://covers.openlibrary.org/b/isbn/9780201616224-L.jpg",
                        1999,
                        campus),
                createBook(
                        "9780137081073",
                        "The Clean Coder",
                        "Robert C. Martin",
                        256,
                        "Een gids over professioneel gedrag, discipline en de ethiek van een softwareontwikkelaar.",
                        "https://covers.openlibrary.org/b/isbn/9780137081073-L.jpg",
                        2011,
                        campus),
                createBook(
                        "9780321125217",
                        "Domain-Driven Design",
                        "Eric Evans",
                        560,
                        "Over het modelleren van complexe software op basis van de onderliggende business-logica.",
                        "https://covers.openlibrary.org/b/isbn/9780321125217-L.jpg",
                        2003,
                        campus),
                createBook(
                        "9780262033848",
                        "Introduction to Algorithms",
                        "Cormen, Leiserson, Rivest & Stein",
                        1312,
                        "Het standaardwerk over algoritmes, datastructuren en computationele complexiteit.",
                        "https://covers.openlibrary.org/b/isbn/9780262033848-L.jpg",
                        2009,
                        campus),
                createBook(
                        "9780134685991",
                        "Effective Java",
                        "Joshua Bloch",
                        416,
                        "De beste praktijken voor het schrijven van robuuste en efficiënte Java-code.",
                        "https://covers.openlibrary.org/b/isbn/9780134685991-L.jpg",
                        2017,
                        campus),
                createBook(
                        "9780134494166",
                        "Design Patterns",
                        "Gamma, Helm, Johnson & Vlissides",
                        395,
                        "De klassieker die de 'Gang of Four' patronen introduceerde in software-architectuur.",
                        "https://covers.openlibrary.org/b/isbn/9780134494166-L.jpg",
                        1994,
                        campus),
                createBook(
                        "9780131103627",
                        "The C Programming Language",
                        "Brian Kernighan & Dennis Ritchie",
                        272,
                        "Het gezaghebbende boek over de programmeertaal C, geschreven door de makers zelf.",
                        "https://covers.openlibrary.org/b/isbn/9780131103627-L.jpg",
                        1988,
                        campus),
                createBook(
                        "9780201835953",
                        "Mythical Man-Month",
                        "Frederick Brooks",
                        336,
                        "Essays over software-engineering en het beheer van complexe softwareprojecten.",
                        "https://covers.openlibrary.org/b/isbn/9780201835953-L.jpg",
                        1975,
                        campus),
                createBook(
                        "9780735619678",
                        "Code Complete",
                        "Steve McConnell",
                        960,
                        "Een praktische gids over softwareconstructie en het bouwen van kwalitatieve code.",
                        "https://covers.openlibrary.org/b/isbn/9780735619678-L.jpg",
                        2004,
                        campus),
                createBook(
                        "9780321356680",
                        "Working Effectively with Legacy Code",
                        "Michael Feathers",
                        456,
                        "Hoe je veilig wijzigingen aanbrengt in grote, ongeteste bestaande codebases.",
                        "https://covers.openlibrary.org/b/isbn/9780321356680-L.jpg",
                        2004,
                        campus),
                createBook(
                        "9781617294532",
                        "Grokking Algorithms",
                        "Aditya Bhargava",
                        256,
                        "Een volledig geïllustreerde, gebruiksvriendelijke gids voor het begrijpen van algoritmes.",
                        "https://covers.openlibrary.org/b/isbn/9781617294532-L.jpg",
                        2016,
                        campus),
                createBook(
                        "9780596517748",
                        "JavaScript: The Good Parts",
                        "Douglas Crockford",
                        176,
                        "Een focus op de beste en krachtigste onderdelen van de programmeertaal JavaScript.",
                        "https://covers.openlibrary.org/b/isbn/9780596517748-L.jpg",
                        2008,
                        campus),
                createBook(
                        "9781491950296",
                        "Building Microservices",
                        "Sam Newman",
                        280,
                        "Een overzicht van het ontwerpen en beheren van gedistribueerde systemen.",
                        "https://covers.openlibrary.org/b/isbn/9781491950296-L.jpg",
                        2015,
                        campus),
                createBook(
                        "9781118831120",
                        "Cracking the Coding Interview",
                        "Gayle Laakmann McDowell",
                        687,
                        "189 programmeervragen en oplossingen om je voor te bereiden op technische interviews.",
                        "https://covers.openlibrary.org/b/isbn/9781118831120-L.jpg",
                        2015,
                        campus),
                createBook(
                        "9780131101630",
                        "Structure and Interpretation of Computer Programs",
                        "Abelson & Sussman",
                        657,
                        "De fundamentele concepten van programmeren via de taal Scheme.",
                        "https://covers.openlibrary.org/b/isbn/9780131101630-L.jpg",
                        1996,
                        campus),
                createBook(
                        "9780321146533",
                        "Test Driven Development: By Example",
                        "Kent Beck",
                        240,
                        "Een introductie tot de methodiek waarbij je tests schrijft voordat je de code schrijft.",
                        "https://covers.openlibrary.org/b/isbn/9780321146533-L.jpg",
                        2002,
                        campus),
                createBook(
                        "9781492045526",
                        "Fluent Python",
                        "Luciano Ramalho",
                        832,
                        "Leer hoe je effectieve en idiomatische Python-code schrijft door de unieke functies van de taal te benutten.",
                        "https://covers.openlibrary.org/b/isbn/9781492045526-L.jpg",
                        2022,
                        campus)));

        System.out.println("BookSeeder klaar: 20 boeken toegevoegd aan campus " + campus.getName());
    }

    private Book createBook(
            String isbn,
            String title,
            String author,
            int pages,
            String description,
            String imageUrl,
            int releaseYear,
            Campus campus) {
        Book book = new Book();

        book.setIsbn(isbn);
        book.setTitle(title);
        book.setAuthor(author);
        book.setPages(pages);
        book.setDescription(description);
        book.setImageUrl(imageUrl);
        book.setReleaseYear(releaseYear);
        book.setCampus(campus);
        book.setDidactic(false);

        return book;
    }
}