package edu.ap.bib_backend.jpa.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import edu.ap.bib_backend.jpa.enums.LoanStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Setter
@Getter
@Table(name = "loans")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate loanDate;
    private LocalDate returnDate;
    private LocalDate returnedDate;

    // Used to group multiple single-copy loans created in one checkout action.
    // Not intended for end-user display.
    private String batchId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_copy_id", nullable = false)
    private BookCopy copy;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campus_id", nullable = false)
    private Campus campus;

    @Column(name = "librarian_id")
    private String librarianId;

    @Column(name = "librarian_notified_at")
    private LocalDateTime librarianNotifiedAt;

    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LoanExtension> extensions = new ArrayList<>();

    public LoanStatus getStatus() {
        if (returnedDate != null) {
            return LoanStatus.RETURNED;
        }

        if (returnDate != null && returnDate.isBefore(LocalDate.now(ZoneId.of("Europe/Brussels")))) {
            return LoanStatus.OVERDUE;
        }

        return LoanStatus.ACTIVE;
    }
}
