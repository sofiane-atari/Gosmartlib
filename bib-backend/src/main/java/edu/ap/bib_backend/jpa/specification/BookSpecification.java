package edu.ap.bib_backend.jpa.specification;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import edu.ap.bib_backend.jpa.entity.Availability;
import edu.ap.bib_backend.jpa.entity.Book;
import jakarta.persistence.criteria.Predicate;

public final class BookSpecification {

    private BookSpecification() {
    }

    public static Specification<Book> withFilters(
            String searchTerm,
            Availability availability,
            Integer minPages,
            Integer maxPages
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (searchTerm != null && !searchTerm.isBlank()) {
                String likePattern = "%" + searchTerm.trim().toLowerCase() + "%";

                Predicate titleMatch = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("title")),
                        likePattern
                );

                Predicate authorMatch = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("author")),
                        likePattern
                );

                predicates.add(criteriaBuilder.or(titleMatch, authorMatch));
            }

            if (availability != null) {
                predicates.add(criteriaBuilder.equal(root.get("availability"), availability));
            }

            if (minPages != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("pages"), minPages));
            }

            if (maxPages != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("pages"), maxPages));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}