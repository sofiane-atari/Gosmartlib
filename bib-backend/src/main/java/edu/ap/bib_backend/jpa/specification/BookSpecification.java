package edu.ap.bib_backend.jpa.specification;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.BookCopy;
import edu.ap.bib_backend.jpa.entity.Genre;
import edu.ap.bib_backend.jpa.entity.Loan;
import edu.ap.bib_backend.jpa.enums.BookCondition;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;

public final class BookSpecification {

    private BookSpecification() {
    }

    public static Specification<Book> withFilters(
            String searchTerm,
            Integer minPages,
            Integer maxPages,
            Long readingLevelId,
            String genreName
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

            if (minPages != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("pages"), minPages));
            }

            if (maxPages != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("pages"), maxPages));
            }

            if (readingLevelId != null) {
                predicates.add(criteriaBuilder.equal(root.get("readingLevel").get("id"), readingLevelId));
            }

            if (genreName != null && !genreName.isBlank()) {
                Join<Book, Genre> genreJoin = root.join("genres");
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(genreJoin.get("name")),
                        genreName.trim().toLowerCase()
                ));
                query.distinct(true);
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Book> belongsToCampus(Long campusId) {
        return (root, query, criteriaBuilder) -> {
            query.distinct(true);
            Join<Book, BookCopy> copyJoin = root.join("bookCopies");
            return criteriaBuilder.equal(copyJoin.get("campus").get("id"), campusId);
        };
    }

    public static Specification<Book> belongsToSchool(Long schoolId) {
        return (root, query, criteriaBuilder) -> {
            query.distinct(true);
            Join<Book, BookCopy> copyJoin = root.join("bookCopies");
            return criteriaBuilder.equal(copyJoin.get("campus").get("school").get("id"), schoolId);
        };
    }

    public static Specification<Book> hasCopyInCampus(Long campusId) {
        return (root, query, criteriaBuilder) -> {
            Join<Book, BookCopy> bookCopyJoin = root.join("bookCopies");
            query.distinct(true);

            return criteriaBuilder.equal(
                    bookCopyJoin.get("campus").get("id"),
                    campusId
            );
        };
    }

    public static Specification<Book> withCandidateFilters(
            String title,
            String author,
            Integer releaseYear
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (title != null && !title.isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("title")),
                        "%" + title.toLowerCase() + "%"
                ));
            }

            if (author != null && !author.isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("author")),
                        "%" + author.toLowerCase() + "%"
                ));
            }

            if (releaseYear != null) {
                predicates.add(criteriaBuilder.equal(root.get("releaseYear"), releaseYear));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
    public static Specification<Book> hasAnyAvailableCopy() {
        return (root, query, criteriaBuilder) -> {
            query.distinct(true);

            jakarta.persistence.criteria.Subquery<Long> availableCopySubquery = query.subquery(Long.class);
            var copyRoot = availableCopySubquery.from(BookCopy.class);
            jakarta.persistence.criteria.Join<BookCopy, Loan> loanJoin = copyRoot.join("loans", jakarta.persistence.criteria.JoinType.LEFT);

            availableCopySubquery.select(copyRoot.get("id"))
                    .where(
                            criteriaBuilder.equal(copyRoot.get("book"), root),
                            criteriaBuilder.notEqual(copyRoot.get("condition"), BookCondition.LOST)
                    )
                    .groupBy(copyRoot.get("id"))
                    .having(criteriaBuilder.equal(
                            criteriaBuilder.sum(
                                    criteriaBuilder.<Integer>selectCase()
                                            .when(criteriaBuilder.and(
                                                    criteriaBuilder.isNotNull(loanJoin.get("id")),
                                                    criteriaBuilder.isNull(loanJoin.get("returnedDate"))
                                            ), 1)
                                            .otherwise(0)
                            ),
                            0
                    ));

            return criteriaBuilder.exists(availableCopySubquery);
        };
    }

    public static Specification<Book> hasNonLostCopyInSchool(Long schoolId) {
        return (root, query, criteriaBuilder) -> {
            query.distinct(true);

            jakarta.persistence.criteria.Subquery<Long> nonLostCopySubquery = query.subquery(Long.class);
            var copyRoot = nonLostCopySubquery.from(BookCopy.class);

            nonLostCopySubquery.select(copyRoot.get("id"))
                    .where(
                            criteriaBuilder.equal(copyRoot.get("book"), root),
                            criteriaBuilder.equal(copyRoot.get("campus").get("school").get("id"), schoolId),
                            criteriaBuilder.notEqual(copyRoot.get("condition"), BookCondition.LOST)
                    );

            return criteriaBuilder.exists(nonLostCopySubquery);
        };
    }

    public static Specification<Book> visibleForUser(boolean canSeeDidacticBooks) {
        return (root, query, criteriaBuilder) -> {
            if (canSeeDidacticBooks) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.isFalse(root.get("didactic"));
        };
    }

    public static Specification<Book> onlyDidactic() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.isTrue(root.get("didactic"));
    }
}
