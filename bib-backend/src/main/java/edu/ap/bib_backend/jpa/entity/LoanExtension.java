package edu.ap.bib_backend.jpa.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import edu.ap.bib_backend.jpa.enums.LoanExtensionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "loan_extensions")
public class LoanExtension {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @Column(name = "old_return_date", nullable = false)
    private LocalDate oldReturnDate;

    @Column(name = "new_return_date", nullable = false)
    private LocalDate newReturnDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "extension_type", nullable = false)
    private LoanExtensionType extensionType;

    @Column(name = "extended_by_user_id", nullable = false)
    private String extendedByUserId;

    @CreationTimestamp
    @Column(name = "extended_at", nullable = false, updatable = false)
    private LocalDateTime extendedAt;
}
