package edu.ap.bib_backend.service.report;

import edu.ap.bib_backend.dto.oneroster.OneRosterClassDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class ReportClassMatcherService {

    public Optional<OneRosterClassDTO> findMatch(
            String classOrGroupName,
            List<OneRosterClassDTO> oneRosterClasses) {
        String normalizedInput = normalize(classOrGroupName);

        if (normalizedInput.isBlank()) {
            return Optional.empty();
        }

        return oneRosterClasses.stream()
                .filter(oneRosterClass -> {
                    String title = normalize(oneRosterClass.title());
                    String classCode = normalize(oneRosterClass.classCode());
                    String sourcedId = normalize(oneRosterClass.sourcedId());

                    return classCode.equals(normalizedInput)
                            || title.equals(normalizedInput)
                            || sourcedId.equals(normalizedInput);
                })
                .findFirst();
    }

    public List<OneRosterClassDTO> findMatches(
            List<String> classOrGroupNames,
            List<OneRosterClassDTO> oneRosterClasses) {
        return classOrGroupNames.stream()
                .map(groupName -> findMatch(groupName, oneRosterClasses))
                .flatMap(Optional::stream)
                .distinct()
                .toList();
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
    }
}