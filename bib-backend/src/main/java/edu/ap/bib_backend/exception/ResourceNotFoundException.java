package edu.ap.bib_backend.exception;

public class ResourceNotFoundException extends RuntimeException {

    private final String fieldName;

    public ResourceNotFoundException(String fieldName, String message) {
        super(message);
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }
}