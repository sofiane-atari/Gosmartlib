package edu.ap.bib_backend.exception;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ApiError {
    private int code;
    private String message;
    private List<ErrorDetail> details;

    public ApiError(int code, String message, List<ErrorDetail> details) {
        this.code = code;
        this.message = message;
        this.details = details;
    }
}
