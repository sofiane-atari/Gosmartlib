package edu.ap.bib_backend.jpa.entity.didactic;

import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.enums.DidacticMaterialType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "didactic_materials")
@Getter
@Setter
public class DidacticMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "material_type", nullable = false, length = 50)
    private DidacticMaterialType materialType;

    @Column(length = 100)
    private String subject;

    @Column(name = "education_level", length = 100)
    private String educationLevel;

    @Column(length = 255)
    private String keywords;

    @Column(name = "created_by_user_id", nullable = false, length = 100)
    private String createdByUserId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "didacticMaterial", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DidacticMaterialBookLink> bookLinks = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    
    public void replaceBookLinks(List<Book> books) {
        this.bookLinks.clear();
        books.forEach(book -> {
            DidacticMaterialBookLink link = new DidacticMaterialBookLink();
            link.setDidacticMaterial(this);
            link.setBook(book);
            this.bookLinks.add(link);
        });
    }
}