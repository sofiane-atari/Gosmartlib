package edu.ap.bib_backend.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.validation.FieldError;

import java.util.ArrayList;
import java.util.List;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceAlreadyExistsException.class) 
    public ResponseEntity<ErrorResponse> handleResourceAlreadyExists(ResourceAlreadyExistsException ex) {
        int errorCode = 409;
        List<ErrorDetail> details = new ArrayList<>();
        ApiError error = new ApiError(errorCode, "Item bestaat al", details);

        details.add(new ErrorDetail(ex.getFieldName(), ex.getMessage()));

        ErrorResponse response = new ErrorResponse(error);
        return ResponseEntity.status(error.getCode()).body(response);
    }

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

    // Handles validation errors from @Valid in controllers
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        List<ErrorDetail> details = new ArrayList<>();

        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            details.add(new ErrorDetail(fieldError.getField(), fieldError.getDefaultMessage()));
        }

        ApiError error = new ApiError(400, "Validation failed", details);
        ErrorResponse response = new ErrorResponse(error);

        return ResponseEntity.badRequest().body(response);
        
    }
}