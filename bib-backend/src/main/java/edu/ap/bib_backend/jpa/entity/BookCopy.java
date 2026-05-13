package edu.ap.bib_backend.jpa.entity;

import java.util.ArrayList;
import java.util.List;

import edu.ap.bib_backend.jpa.enums.Availability;
import org.hibernate.annotations.Formula;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Entity
@Setter
@Getter
@Table(name = "book_copies")
public class BookCopy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Setter(AccessLevel.NONE)
    @Enumerated(EnumType.STRING)
    @Formula("""
            (
                CASE
                    WHEN EXISTS (
                        SELECT 1
                        FROM loan_copies lc
                        JOIN loans l ON l.id = lc.loan_id
                        WHERE lc.book_copy_id = id
                          AND l.returned_date IS NULL
                    ) THEN 'BORROWED'
                    ELSE 'AVAILABLE'
                END
            )
            """)
    private Availability availability;

    @ManyToMany(mappedBy = "copies")
    private List<Loan> loans = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campus_id", nullable = false)
    private Campus campus;

    @ManyToOne
    @JoinColumn(name = "school_id")
    private School school;
}
