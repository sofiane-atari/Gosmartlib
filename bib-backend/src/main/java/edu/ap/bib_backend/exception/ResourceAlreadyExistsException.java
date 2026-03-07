package edu.ap.bib_backend.exception;

public class ResourceAlreadyExistsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private String fieldName; // The field that caused the conflict
    private Object fieldValue; // The value that already exists

    public ResourceAlreadyExistsException(String fieldName, Object fieldValue) {
        super(String.format("Resource bestaat al met %s: '%s'", fieldName, fieldValue));
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    public ResourceAlreadyExistsException(String fieldName, Object fieldValue, String message) {
        super(message);
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Object getFieldValue() {
        return fieldValue;
    }
}