package edu.ap.bib_backend.jpa.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Setter
@Getter
@Table(name = "book_loans")
public class BookLoan {

    @Id
    private Long id;

    @ManyToOne
    @JoinColumn(name = "book_copy_id") // FK stored in book_loans table
    private BookCopy copy;

    @ManyToOne
    @JoinColumn(name = "user_id") 
    private User user;
}