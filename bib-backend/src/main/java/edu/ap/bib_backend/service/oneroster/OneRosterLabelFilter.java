package edu.ap.bib_backend.service.oneroster;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

class OneRosterLabelFilter {
    List<String> filterUmbrellaClassLabels(List<String> labels) {
        if (labels.size() < 2) {
            return labels;
        }

        List<String> filtered = new ArrayList<>();
        for (String candidate : labels) {
            if (!isUmbrellaClassLabel(candidate, labels)) {
                filtered.add(candidate);
            }
        }
        return filtered;
    }

    private boolean isUmbrellaClassLabel(String candidate, List<String> labels) {
        String candidateLower = candidate.toLowerCase(Locale.ROOT);

        for (String other : labels) {
            if (candidate.equals(other)) {
                continue;
            }

            String otherLower = other.toLowerCase(Locale.ROOT);
            if (!otherLower.startsWith(candidateLower) || other.length() <= candidate.length()) {
                continue;
            }

            char nextChar = other.charAt(candidate.length());
            if (Character.isDigit(nextChar) || nextChar == '-' || nextChar == '_' || nextChar == '/' || nextChar == '.') {
                return true;
            }
        }

        return false;
    }
}
