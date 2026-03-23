package edu.ap.bib_backend.dto;

import lombok.Getter;

@Getter
public class FileValidationError {
    private int row;
    private String field;
    private String message;

    public FileValidationError(int row, String field, String message) {
        this.row = row;
        this.field = field;
        this.message = message;
    }

}