package edu.ap.bib_backend.dto;

public class CampusDTO {

    private Long id;
    private String name;
    private String code;
    private Long schoolId;
    private String schoolName;
    private boolean defaultCampus;

    public CampusDTO() {}

    public CampusDTO(Long id, String name, String code, Long schoolId, String schoolName, boolean defaultCampus) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.schoolId = schoolId;
        this.schoolName = schoolName;
        this.defaultCampus = defaultCampus;
    }

    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }

    public void setCode(String code) { this.code = code; }

    public Long getSchoolId() { return schoolId; }

    public void setSchoolId(Long schoolId) { this.schoolId = schoolId; }

    public String getSchoolName() { return schoolName; }

    public void setSchoolName(String schoolName) { this.schoolName = schoolName; }

    public boolean isDefaultCampus() { return defaultCampus; }

    public void setDefaultCampus(boolean defaultCampus) {
        this.defaultCampus = defaultCampus;
    }
}