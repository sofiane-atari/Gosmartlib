package edu.ap.bib_backend.jpa.entity;

import jakarta.persistence.Column;
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
import lombok.NoArgsConstructor;
import lombok.Setter;
import edu.ap.bib_backend.jpa.converter.JasyptStringAttributeConverter;
import jakarta.persistence.Convert;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "oneroster_credentials", uniqueConstraints = {
        @UniqueConstraint(name = "uk_oneroster_credentials_school", columnNames = "school_id")
})
public class OneRosterCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false, unique = true)
    private School school;

    @Column(name = "client_id", nullable = false)
    @Convert(converter = JasyptStringAttributeConverter.class)
    private String clientId;

    @Column(name = "client_secret", nullable = false)
    @Convert(converter = JasyptStringAttributeConverter.class)
    private String clientSecret;
}
