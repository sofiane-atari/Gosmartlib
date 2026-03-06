package edu.ap.bib_backend.exception;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ErrorDetail {
    private String field;
    private String message;

    public ErrorDetail(String field, String message) {
        this.field = field;
        this.message = message;
    }
}
