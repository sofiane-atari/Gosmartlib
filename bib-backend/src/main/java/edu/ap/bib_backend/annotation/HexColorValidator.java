package edu.ap.bib_backend.annotation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

// Validator for the @HexColor annotation to ensure that the color string is a valid hex code (e.g., #FFFFFF or #FFF)
public class HexColorValidator implements ConstraintValidator<HexColor, String> {

    private static final String HEX_PATTERN = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$";

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return false;
        return value.matches(HEX_PATTERN);
    }
}