package edu.ap.bib_backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReadingLevelDTO {

    private Long id;
    private String niveauName;
    private String description;

    public ReadingLevelDTO() {
    }

    public ReadingLevelDTO(Long id, String niveauName, String description) {
        this.id = id;
        this.niveauName = niveauName;
        this.description = description;
    }

    @Override
    public String toString() {
        return "ReadingLevelDTO{" +
                "id=" + id +
                ", niveauName='" + niveauName + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}