package edu.ap.bib_backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class CampusDTO {

    private Long id;
    private String name;
    private Long schoolId;
    private String schoolName;
    private String address;
    @NotNull(message = "Postcode is verplicht.")
    @Min(value = 1, message = "Postcode moet groter zijn dan 0.")
    private Integer postalCode;
    private String contactPhone;
    @Email(message = "Vul een geldig e-mailadres in.")
    private String contactEmail;

    public CampusDTO() {}

    public CampusDTO(
            Long id,
            String name,
            Long schoolId,
            String schoolName,
            String address,
            Integer postalCode,
            String contactPhone,
            String contactEmail
    ) {
        this.id = id;
        this.name = name;
        this.schoolId = schoolId;
        this.schoolName = schoolName;
        this.address = address;
        this.postalCode = postalCode;
        this.contactPhone = contactPhone;
        this.contactEmail = contactEmail;
    }

    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public Long getSchoolId() { return schoolId; }

    public void setSchoolId(Long schoolId) { this.schoolId = schoolId; }

    public String getSchoolName() { return schoolName; }

    public void setSchoolName(String schoolName) { this.schoolName = schoolName; }

    public String getAddress() { return address; }

    public void setAddress(String address) { this.address = address; }

    public Integer getPostalCode() { return postalCode; }

    public void setPostalCode(Integer postalCode) { this.postalCode = postalCode; }

    public String getContactPhone() { return contactPhone; }

    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public String getContactEmail() { return contactEmail; }

    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
}
