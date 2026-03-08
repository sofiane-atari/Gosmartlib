package edu.ap.bib_backend.jpa.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "reading_list_books")
public class ReadingListBook {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "reading_list_id", nullable = false)
    @JsonIgnore
    private ReadingList readingList;
    @Column(nullable = false)
    private Long bookId;

}
