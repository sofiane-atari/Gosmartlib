package edu.ap.bib_backend.dto;

import java.util.List;

import lombok.Getter;

@Getter
public class FileValidationResult {
    private int totalRows;
    private int validRows;
    private int invalidRows;
    private List<FileValidationError> errors;

    public FileValidationResult(int totalRows, int validRows, int invalidRows, List<FileValidationError> errors) {
        this.totalRows = totalRows;
        this.validRows = validRows;
        this.invalidRows = invalidRows;
        this.errors = errors;
    }
}