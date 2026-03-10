package edu.ap.bib_backend.options;

import org.springframework.lang.Nullable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class LabelGetOptions {

    private boolean includeBooksCount = false;
    private String keyword = "";
}
