package edu.ap.bib_backend.jpa.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;

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
@Table(name = "book_copies")
public class BookCopy {

    @Id 
    private Long id;

    
    @ManyToOne
    @JoinColumn(name = "book_id") // FK stored in book_copies table
    private Book book;

    @OneToMany(mappedBy = "copy")
    private List<BookLoan> loans = new ArrayList<>();
}