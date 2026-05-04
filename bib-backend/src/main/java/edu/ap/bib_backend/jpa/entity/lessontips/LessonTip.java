package edu.ap.bib_backend.jpa.entity.lessontips;

import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.enums.LessonTipType;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "lesson_tips")
@Getter
@Setter
public class LessonTip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "lesson_tip_type", nullable = false, length = 50)
    private LessonTipType lessonTipType;

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

    @OneToMany(mappedBy = "lessonTip", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LessonTipBookLink> bookLinks = new ArrayList<>();

    @OneToMany(mappedBy = "lessonTip", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LessonTipAttachment> attachments = new ArrayList<>();

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
        Set<Long> requestedBookIds = books.stream()
                .map(Book::getId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        this.bookLinks.removeIf(link ->
                link.getBook() == null
                        || link.getBook().getId() == null
                        || !requestedBookIds.contains(link.getBook().getId())
        );

        Set<Long> existingBookIds = this.bookLinks.stream()
                .map(LessonTipBookLink::getBook)
                .filter(java.util.Objects::nonNull)
                .map(Book::getId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        for (Book book : books) {
            Long bookId = book.getId();

            if (bookId == null || existingBookIds.contains(bookId)) {
                continue;
            }

            LessonTipBookLink link = new LessonTipBookLink();
            link.setLessonTip(this);
            link.setBook(book);
            this.bookLinks.add(link);
        }
    }
}