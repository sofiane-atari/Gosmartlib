package edu.ap.bib_backend.exception;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ErrorResponse {
    private ApiError error;

    public ErrorResponse(ApiError error) {
        this.error = error;
    }
}
