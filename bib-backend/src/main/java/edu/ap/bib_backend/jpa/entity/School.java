package edu.ap.bib_backend.jpa.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import edu.ap.bib_backend.dto.SchoolSummaryDto;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "schools")
@Entity
public class School {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "domain_name", nullable = false)
    private String domainName;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address = "";

    @Column(nullable = false)
    private Integer postalCode = 0;

    @Column(name = "contact_phone", length = 50)
    private String contactPhone;

    @Column(name = "contact_email", length = 255)
    private String contactEmail;

    @Column(name = "default_loan_duration_days", nullable = false)
    private Integer defaultLoanDurationDays = 15;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // New: timestamp of the last successful OneRoster sync for this school
    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;

    @OneToMany(mappedBy = "school", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Campus> campuses = new ArrayList<>();

    public SchoolSummaryDto getSchoolSummaryDto() {
        return new SchoolSummaryDto(id, name, defaultLoanDurationDays);
    }
}
