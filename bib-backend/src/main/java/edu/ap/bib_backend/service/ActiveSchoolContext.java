package edu.ap.bib_backend.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@Component
@RequestScope
public class ActiveSchoolContext {

    private Long activeSchoolId;
    private String activeSchoolDomainName;
    private List<String> activeClassCodes = new ArrayList<>();
    private List<String> activeClassNames = new ArrayList<>();
    private List<String> activePrimaryGroupNames = new ArrayList<>();

    public boolean isClassOverrideActive() {
        return !activeClassCodes.isEmpty() || !activeClassNames.isEmpty() || !activePrimaryGroupNames.isEmpty();
    }
}
