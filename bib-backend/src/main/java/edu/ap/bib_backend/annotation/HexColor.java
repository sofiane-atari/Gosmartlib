package edu.ap.bib_backend.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

// Custom annotation to validate that a string is a valid hex color code (e.g., #FFFFFF or #FFF)
@Documented
@Constraint(validatedBy = HexColorValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface HexColor {
    String message() default "ongeldige hex kleurcode";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}