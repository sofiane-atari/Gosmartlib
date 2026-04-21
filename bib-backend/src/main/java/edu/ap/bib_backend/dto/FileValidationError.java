package edu.ap.bib_backend.dto;

import lombok.Getter;

@Getter
public class FileValidationError {
    private int row;
    private String field;
    private String message;
    private String severity; // "error", "warning", "info"

    public FileValidationError(int row, String field, String message) {
        this(row, field, message, "error");
    }

    public FileValidationError(int row, String field, String message, String severity) {
        this.row = row;
        this.field = field;
        this.message = message;
        this.severity = severity;
    }

}