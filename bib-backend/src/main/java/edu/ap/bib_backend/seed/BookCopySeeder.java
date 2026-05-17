package edu.ap.bib_backend.seed;

import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.BookCopy;
import edu.ap.bib_backend.jpa.repository.BookCopyRepository;
import edu.ap.bib_backend.jpa.repository.BookRepository;
import jakarta.transaction.Transactional;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Order(3)
public class BookCopySeeder implements CommandLineRunner {

    private static final int COPIES_PER_BOOK = 3;

    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;

    public BookCopySeeder(
            BookRepository bookRepository,
            BookCopyRepository bookCopyRepository) {
        this.bookRepository = bookRepository;
        this.bookCopyRepository = bookCopyRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        List<Book> books = bookRepository.findAll();

        if (books.isEmpty()) {
            System.out.println("BookCopySeeder overgeslagen: er bestaan nog geen boeken.");
            return;
        }

        List<BookCopy> bookCopiesToCreate = new ArrayList<>();

        for (Book book : books) {
            long existingCopies = bookCopyRepository.countByBookId(book.getId());
            long missingCopies = COPIES_PER_BOOK - existingCopies;

            if (missingCopies <= 0) {
                continue;
            }

            for (int i = 0; i < missingCopies; i++) {
                bookCopiesToCreate.add(createBookCopy(book));
            }
        }

        if (bookCopiesToCreate.isEmpty()) {
            System.out.println("BookCopySeeder overgeslagen: elk boek heeft al 3 exemplaren.");
            return;
        }

        bookCopyRepository.saveAll(bookCopiesToCreate);

        System.out.println("BookCopySeeder klaar: " + bookCopiesToCreate.size() + " exemplaren toegevoegd.");
    }

    private BookCopy createBookCopy(Book book) {
        BookCopy bookCopy = new BookCopy();

        bookCopy.setBook(book);
        bookCopy.setCampus(book.getCampus());

        return bookCopy;
    }
}