package edu.ap.bib_backend.jpa.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "reading_lists")
public class ReadingList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campus_id", nullable = false)
    private Campus campus;

    @OneToMany(mappedBy = "readingList", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ReadingListBook> books;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private ReadingListTargetType targetType = ReadingListTargetType.PERSONAL;

    @Column(name = "target_value")
    private String targetValue;

    @Column(name = "public_share_id", unique = true, length = 64)
    private String publicShareId;

    @Column(name = "share_enabled", nullable = false)
    private boolean shareEnabled = false;
}