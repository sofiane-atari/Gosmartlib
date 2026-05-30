package edu.ap.bib_backend.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Setter
@Getter
@Component
@RequestScope
public class ActiveSchoolContext {

    private Long activeSchoolId;
}
