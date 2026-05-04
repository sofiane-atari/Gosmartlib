package edu.ap.bib_backend.service.smartschool;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class SmartschoolAudienceResolverService {

    public static final String FALLBACK_CLASS_CODE = "GENERAL";
    public static final String FALLBACK_YEAR_CODE = "0";
    public static final String FALLBACK_SCHOOL_CODE = "ALLEMAAL";

    public Map<String, Object> toStringObjectMap(Map<?, ?> source) {
        Map<String, Object> result = new HashMap<>();
        if (source == null) {
            return result;
        }

        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (entry.getKey() instanceof String key) {
                result.put(key, entry.getValue());
            }
        }
        return result;
    }

    public String resolveClassCode(Map<String, Object> attributes) {
        String[] candidateKeys = {
                "klas", "class", "classCode", "className", "classname",
                "klasnaam", "stamboekgroep", "groep", "group", "groupName", "groups", "klassen"
        };

        for (String key : candidateKeys) {
            String resolved = extractClassValue(attributes.get(key));
            if (resolved != null) {
                return resolved;
            }
        }

        return FALLBACK_CLASS_CODE;
    }

    public String resolveYearCode(Map<String, Object> attributes, String classCode) {
        String[] yearKeys = { "year", "schoolYear", "jaar", "yearCode", "grade" };
        for (String key : yearKeys) {
            Object value = attributes.get(key);
            if (value instanceof String str && !str.isBlank()) {
                return str.trim();
            }
        }

        String source = classCode == null ? "" : classCode.trim();
        int i = 0;
        while (i < source.length() && Character.isDigit(source.charAt(i))) {
            i++;
        }
        if (i > 0) {
            return source.substring(0, i);
        }

        return FALLBACK_YEAR_CODE;
    }

    public String resolveSchoolCode(Map<String, Object> attributes) {
        Object platform = attributes.get("platform");
        if (platform instanceof String str && !str.isBlank()) {
            return str.trim();
        }
        return FALLBACK_SCHOOL_CODE;
    }

    private String extractClassValue(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof String str) {
            String trimmed = str.trim();
            return trimmed.isEmpty() ? null : trimmed;
        }

        if (value instanceof Map<?, ?> mapValue) {
            String[] nestedKeys = { "name", "naam", "code", "id", "groupName", "className" };
            for (String nestedKey : nestedKeys) {
                Object nested = mapValue.get(nestedKey);
                String resolved = extractClassValue(nested);
                if (resolved != null) {
                    return resolved;
                }
            }
            return null;
        }

        if (value instanceof Iterable<?> items) {
            for (Object item : items) {
                String resolved = extractClassValue(item);
                if (resolved != null) {
                    return resolved;
                }
            }
        }

        return null;
    }
}

