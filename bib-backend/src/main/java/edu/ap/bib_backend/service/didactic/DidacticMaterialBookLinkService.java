package edu.ap.bib_backend.service.didactic;

import edu.ap.bib_backend.exception.ResourceNotFoundException;
import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.didactic.DidacticMaterial;
import edu.ap.bib_backend.jpa.entity.didactic.DidacticMaterialBookLink;
import edu.ap.bib_backend.jpa.repository.BookRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DidacticMaterialBookLinkService {

    private final BookRepository bookRepository;

    public DidacticMaterialBookLinkService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public void replaceBookLinks(DidacticMaterial material, List<Long> linkedBookIds) {
        List<Long> normalizedIds = normalizeLinkedBookIds(linkedBookIds);
        List<Book> linkedBooks = normalizedIds.isEmpty() ? List.of() : loadLinkedBooks(normalizedIds);
        material.replaceBookLinks(linkedBooks);
    }

    private List<Long> normalizeLinkedBookIds(List<Long> linkedBookIds) {
        if (linkedBookIds == null || linkedBookIds.isEmpty()) {
            return List.of();
        }

        Set<Long> uniqueIds = linkedBookIds.stream()
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return new ArrayList<>(uniqueIds);
    }

    private List<Book> loadLinkedBooks(List<Long> linkedBookIds) {
        List<Book> linkedBooks = bookRepository.findAllById(linkedBookIds);

        if (linkedBooks.size() != linkedBookIds.size()) {
            Set<Long> foundBookIds = linkedBooks.stream()
                    .map(Book::getId)
                    .collect(Collectors.toSet());

            Long missingBookId = linkedBookIds.stream()
                    .filter(bookId -> !foundBookIds.contains(bookId))
                    .findFirst()
                    .orElse(null);

            throw new ResourceNotFoundException(
                    "linkedBookIds",
                    "Boek niet gevonden met id: " + missingBookId
            );
        }

        return linkedBooks;
    }
}