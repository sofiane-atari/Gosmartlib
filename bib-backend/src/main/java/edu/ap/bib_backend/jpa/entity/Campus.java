package edu.ap.bib_backend.jpa.entity;

import jakarta.persistence.*;

@Entity
@Table(
        name = "campuses",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_campus_school_name", columnNames = {"school_id", "name"}),
                @UniqueConstraint(name = "uk_campus_school_code", columnNames = {"school_id", "code"})
        }
)
public class Campus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private boolean defaultCampus = false;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }

    public void setCode(String code) { this.code = code; }

    public boolean isDefaultCampus() { return defaultCampus; }

    public void setDefaultCampus(boolean defaultCampus) {
        this.defaultCampus = defaultCampus;
    }

    public School getSchool() { return school; }

    public void setSchool(School school) { this.school = school; }
}