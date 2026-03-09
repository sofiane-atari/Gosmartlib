package edu.ap.bib_backend.options;

import org.springframework.lang.Nullable;

public record LabelGetOptions(@Nullable boolean includeBookCount) {
}