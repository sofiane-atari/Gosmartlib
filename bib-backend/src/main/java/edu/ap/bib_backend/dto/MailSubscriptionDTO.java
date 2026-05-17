package edu.ap.bib_backend.dto;

import java.time.LocalDateTime;

import edu.ap.bib_backend.jpa.enums.MailType;
import lombok.Builder;

@Builder
public record MailSubscriptionDTO(
    Long id,
    Long bookId,
    String bookTitle,
    MailType mailType,
    LocalDateTime createdAt
) {
}
