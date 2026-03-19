package edu.ap.bib_backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookPreviewDTO {
    private boolean previewAvailable;
    private String previewUrl;
    private String detailsUrl;
    private String message;
    private String embedUrl;

    public BookPreviewDTO() {
    }

    public BookPreviewDTO(boolean previewAvailable, String previewUrl, String detailsUrl, String message, String embedUrl) {
        this.previewAvailable = previewAvailable;
        this.previewUrl = previewUrl;
        this.detailsUrl = detailsUrl;
        this.message = message;
        this.embedUrl = embedUrl;
    }
}
