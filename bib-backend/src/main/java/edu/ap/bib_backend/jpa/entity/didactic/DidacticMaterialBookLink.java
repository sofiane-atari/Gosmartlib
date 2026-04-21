package edu.ap.bib_backend.jpa.entity.didactic;

import edu.ap.bib_backend.jpa.entity.Book;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "didactic_material_book_links",
        uniqueConstraints = @UniqueConstraint(columnNames = { "didactic_material_id", "book_id" }))
@Getter
@Setter
public class DidacticMaterialBookLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "didactic_material_id", nullable = false)
    private DidacticMaterial didacticMaterial;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;
}