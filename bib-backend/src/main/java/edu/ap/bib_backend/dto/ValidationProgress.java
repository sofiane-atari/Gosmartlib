package edu.ap.bib_backend.dto;

import java.util.List;

import lombok.Getter;

@Getter
public class ValidationProgress {

    private int processedRows;
    private int totalRows;
    private int progress;
    private List<FileValidationError> errors;

    public ValidationProgress(int processedRows, int totalRows, int progress, List<FileValidationError> errors) {
        this.processedRows = processedRows;
        this.totalRows = totalRows;
        this.progress = progress;
        this.errors = errors;
    }
}
