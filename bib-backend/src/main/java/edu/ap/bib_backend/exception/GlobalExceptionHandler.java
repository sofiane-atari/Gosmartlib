package edu.ap.bib_backend.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import java.util.ArrayList;
import java.util.List;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        int errorCode = 400;
        List<ErrorDetail> details = new ArrayList<>();
        ApiError error = new ApiError(errorCode, ex.getMessage(), details);

        ErrorResponse response = new ErrorResponse(error);
        return ResponseEntity.status(error.getCode()).body(response);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntime(RuntimeException ex) {
        int errorCode = 500;
        List<ErrorDetail> details = new ArrayList<>();
        ApiError error = new ApiError(errorCode, ex.getMessage(), details);

        ErrorResponse response = new ErrorResponse(error);
        return ResponseEntity.status(error.getCode()).body(response);
    }
}