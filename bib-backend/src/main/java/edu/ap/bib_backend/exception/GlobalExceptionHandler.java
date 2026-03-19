package edu.ap.bib_backend.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
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

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex) {
        int errorCode = ex.getStatusCode().value();
        List<ErrorDetail> details = new ArrayList<>();
        String message = ex.getReason() != null ? ex.getReason() : "Er is een fout opgetreden";
        ApiError error = new ApiError(errorCode, message, details);

        ErrorResponse response = new ErrorResponse(error);
        return ResponseEntity.status(errorCode).body(response);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntime(RuntimeException ex) {
        ResponseStatusException nestedResponseStatus = findNestedResponseStatusException(ex);
        if (nestedResponseStatus != null) {
            return handleResponseStatus(nestedResponseStatus);
        }

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

        ApiError error = new ApiError(400, "Validatie mislukt", details);
        ErrorResponse response = new ErrorResponse(error);

        return ResponseEntity.badRequest().body(response);

    }

    // Sends the field that is not found with the custom message send by the thrower
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        int errorCode = 404;
        List<ErrorDetail> details = new ArrayList<>();
        ApiError error = new ApiError(errorCode, "Item niet gevonden", details);

        details.add(new ErrorDetail(ex.getFieldName(), ex.getMessage()));

        ErrorResponse response = new ErrorResponse(error);
        return ResponseEntity.status(error.getCode()).body(response);
    }

    // Handles database constraint errors (e.g. foreign key conflicts, duplicate
    // values)
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            org.springframework.dao.DataIntegrityViolationException ex) {

        int errorCode = 409; // HTTP 409 Conflict
        List<ErrorDetail> details = new ArrayList<>();
        String userMessage;
        String detailMessage;

        // Unwrap the Spring exception to get the actual SQL error message
        String rootMessage = ex.getRootCause() != null
                ? ex.getRootCause().getMessage().toLowerCase()
                : "";

        if (rootMessage.contains("foreign key") || rootMessage.contains("fk_")) {
            userMessage = "Dit item kan niet worden verwijderd omdat het nog gekoppeld is aan andere gegevens.";
            detailMessage = extractForeignKeyDetail(rootMessage);

        } else if (rootMessage.contains("unique") || rootMessage.contains("duplicate entry")) {
            userMessage = "Dit item bestaat al. Controleer de invoer op dubbele waarden.";
            detailMessage = extractUniqueViolationDetail(rootMessage);

        } else if (rootMessage.contains("null") || rootMessage.contains("not null")) {
            userMessage = "Een verplicht veld ontbreekt. Vul alle verplichte velden in.";
            detailMessage = "Er is geprobeerd een lege waarde op te slaan in een verplicht veld.";

        } else if (rootMessage.contains("truncated") || rootMessage.contains("too long")) {
            userMessage = "Een ingevoerde waarde is te lang. Controleer de veldlengtes.";
            detailMessage = "De waarde overschrijdt de maximale lengte die is toegestaan voor dit veld.";

        } else {
            // Unknown DB error — don't expose raw SQL to the user
            userMessage = "De actie kon niet worden uitgevoerd vanwege een conflict met bestaande gegevens.";
            detailMessage = "Controleer de ingevoerde gegevens en probeer het opnieuw.";
        }

        ApiError error = new ApiError(errorCode, userMessage, details);
        details.add(new ErrorDetail("database", detailMessage));
        ErrorResponse response = new ErrorResponse(error);
        return ResponseEntity.status(error.getCode()).body(response);
    }

    // Tries to extract the referenced table name from the error, e.g:
    // "foreign key (`order_id`) references `orders`" → "orders"
    private String extractForeignKeyDetail(String message) {
        if (message.contains("references")) {
            try {
                String table = message.replaceAll(".*references `?(\\w+)`?.*", "$1");
                return String.format(
                        "Verwijder eerst de gekoppelde items in '%s' voordat u dit item verwijdert.", table);
            } catch (Exception ignored) {
            }
        }
        return "Verwijder of ontkoppel eerst alle gerelateerde items voordat u dit item verwijdert.";
    }

    // Tries to extract the duplicate value and field name from the error, e.g:
    // "duplicate entry 'john@example.com' for key 'email'" → value + field
    private String extractUniqueViolationDetail(String message) {
        try {
            if (message.contains("duplicate entry")) {
                String value = message.replaceAll(".*duplicate entry '([^']+)'.*", "$1");
                String field = message.replaceAll(".*for key '([^']+)'.*", "$1");
                return String.format("De waarde '%s' bestaat al voor het veld '%s'.", value, field);
            }
        } catch (Exception ignored) {
        }
        return "Een van de ingevoerde waarden bestaat al in het systeem. Gebruik een unieke waarde.";
    }

    private ResponseStatusException findNestedResponseStatusException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ResponseStatusException responseStatusException) {
                return responseStatusException;
            }
            current = current.getCause();
        }
        return null;
    }
}