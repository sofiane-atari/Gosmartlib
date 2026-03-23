package edu.ap.bib_backend.jpa.entity;

import java.util.ArrayList;
import java.util.List;

import edu.ap.bib_backend.annotation.HexColor;
import edu.ap.bib_backend.projection.GenreProjection;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "genres")
public class Genre implements GenreProjection {

    @Id
    @Column(nullable = false)
    @NotBlank
    private String name;

    @Column(nullable = false)
    @NotBlank
    @HexColor
    private String color;

    private String description;

    @ManyToMany(mappedBy = "genres")
    private List<Book> books = new ArrayList<>();

    public Genre() {
    }

    public Genre(String name, String color, String description) {
        this.name = name;
        this.color = color;
        this.description = description;
    }
}