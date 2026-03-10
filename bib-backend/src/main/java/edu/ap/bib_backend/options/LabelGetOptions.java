package edu.ap.bib_backend.options;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class LabelGetOptions {

    private boolean includeBookCount = false;
    private String search = "";
    private Integer bookCount = null;
}
