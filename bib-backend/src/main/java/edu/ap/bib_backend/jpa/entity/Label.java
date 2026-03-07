package edu.ap.bib_backend.jpa.entity;

import edu.ap.bib_backend.annotation.HexColor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "labels")
public class Label {

    @Id
    @Column(nullable = false)
    @NotBlank
    private String name;

    @Column(nullable = false)
    @NotBlank
    @HexColor
    private String color;

    private String description;

    public Label() {
    }

    public Label(String name,  String color, String description) {
        this.name = name;
        this.color = color;
        this.description = description;
    }
}