package edu.ap.bib_backend.jpa.entity;

import edu.ap.bib_backend.jpa.enums.Availability;
import edu.ap.bib_backend.jpa.enums.BookCondition;
import org.hibernate.annotations.Formula;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
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

    @Column(name = "public_identifier", length = 13, unique = true)
    private String publicIdentifier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Enumerated(EnumType.STRING)
    @Column(name = "physical_condition", nullable = false, columnDefinition = "varchar(255) default 'GOOD_CONDITION'")
    private BookCondition condition = BookCondition.GOOD_CONDITION;

    @Setter(AccessLevel.NONE)
    @Enumerated(EnumType.STRING)
    @Formula("""
             (
                 CASE
                   WHEN physical_condition = 'LOST' THEN 'UNAVAILABLE'
                   WHEN EXISTS (
                     SELECT 1
                      FROM loans l
                      WHERE l.book_copy_id = id
                        AND l.returned_date IS NULL
                    ) THEN 'BORROWED'
                    ELSE 'AVAILABLE'
                  END
             )
             """)
    private Availability availability;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campus_id", nullable = false)
    private Campus campus;

    @OneToMany(mappedBy = "copy")
    private List<Loan> loans = new ArrayList<>();
}
