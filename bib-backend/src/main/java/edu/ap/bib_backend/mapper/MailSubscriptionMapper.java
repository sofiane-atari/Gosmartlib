package edu.ap.bib_backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import edu.ap.bib_backend.dto.MailSubscriptionDTO;
import edu.ap.bib_backend.jpa.entity.MailSubscription;

@Mapper(componentModel = "spring")
public interface MailSubscriptionMapper {

    @Mapping(target = "bookId", source = "book.id")
    @Mapping(target = "bookTitle", source = "book.title")
    MailSubscriptionDTO toDto(MailSubscription entity);
}
