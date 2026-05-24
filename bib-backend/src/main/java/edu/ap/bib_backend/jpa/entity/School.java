package edu.ap.bib_backend.jpa.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import edu.ap.bib_backend.service.api.ProviderType;

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

    @Column(name = "domain_name", nullable = false, unique = true)
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

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", nullable = false)
    private ProviderType providerType = ProviderType.REAL;

    @OneToMany(mappedBy = "school", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Campus> campuses = new ArrayList<>();

    public SchoolSummaryDto getSchoolSummaryDto() {
        return new SchoolSummaryDto(id, name, defaultLoanDurationDays);
    }
}
