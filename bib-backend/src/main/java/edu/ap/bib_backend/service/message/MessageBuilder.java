package edu.ap.bib_backend.service.message;

public interface MessageBuilder {
    MessageBuilder addComponent(MessageComponent component);
    String build();
}
