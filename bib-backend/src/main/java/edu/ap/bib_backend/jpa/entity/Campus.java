package edu.ap.bib_backend.jpa.entity;

import jakarta.persistence.*;

@Entity
@Table(
        name = "campuses",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_campus_school_name", columnNames = {"school_id", "name"})
        }
)
public class Campus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column
    private String address;

    @Column
    private Integer postalCode;

    @Column(name = "contact_phone", length = 50)
    private String contactPhone;

    @Column(name = "contact_email", length = 255)
    private String contactEmail;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }

    public void setAddress(String address) { this.address = address; }

    public Integer getPostalCode() { return postalCode; }

    public void setPostalCode(Integer postalCode) { this.postalCode = postalCode; }

    public String getContactPhone() { return contactPhone; }

    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public String getContactEmail() { return contactEmail; }

    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public School getSchool() { return school; }

    public void setSchool(School school) { this.school = school; }
}
