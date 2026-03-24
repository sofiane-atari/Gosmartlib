package edu.ap.bib_backend.jpa.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "ReadingLevels")
@Getter
@Setter
public class ReadingLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String niveauName;

    private String description;

    @OneToMany(mappedBy = "readingLevel")
    private List<Book> books = new ArrayList<>();

    public ReadingLevel() {
    }

    public ReadingLevel(String niveauName, String description) {
        this.niveauName = niveauName;
        this.description = description;
    }
}
