package edu.ap.bib_backend.seed.dev;

import edu.ap.bib_backend.dto.TagDTO;
import edu.ap.bib_backend.service.TagService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class TagDevSeeder {

    private final TagService tagService;

    public TagDevSeeder(TagService tagService) {
        this.tagService = tagService;
    }

    private static final List<TagData> TAG_DATA = List.of(
            new TagData("Python", "Programmeertaal Python en gerelateerde onderwerpen"),
            new TagData("Java", "Programmeertaal Java en gerelateerde onderwerpen"),
            new TagData("JavaScript", "Programmeertaal JavaScript en gerelateerde onderwerpen"),
            new TagData("Algoritmes", "Algoritmen, datastructuren en computationeel denken"),
            new TagData("Webontwikkeling", "Webdevelopment, frontend en backend"),
            new TagData("Databanken", "Databanken, SQL en datamodellering"),
            new TagData("Cyberbeveiliging", "Cybersecurity, netwerkbeveiliging en ethisch hacken"),
            new TagData("Machine Learning", "Machine learning, kunstmatige intelligentie en data science"),
            new TagData("Software-architectuur", "Softwarearchitectuur, ontwerppatronen en systeemontwerp"),
            new TagData("DevOps", "DevOps, CI/CD, containerisatie en infrastructuurautomatisering")
    );

    public TagSeedResult seed(Authentication authentication) {
        Map<String, Long> tagNameToId = new HashMap<>();
        int created = 0;

        for (TagData data : TAG_DATA) {
            try {
                TagDTO dto = new TagDTO(null, data.name, data.description, null, null);
                TagDTO createdTag = tagService.createTag(dto, authentication);
                tagNameToId.put(createdTag.name(), createdTag.id());
                created++;
                log.info("Tag aangemaakt: {}", data.name);
            } catch (IllegalArgumentException e) {
                if (e.getMessage() != null && e.getMessage().contains("bestaat al")) {
                    log.info("Tag overgeslagen (bestaat al): {}", data.name);
                } else {
                    log.warn("Kon tag niet aanmaken '{}': {}", data.name, e.getMessage());
                }
            }
        }

        log.info("Tag seeding voltooid: {} van {} tags aangemaakt", created, TAG_DATA.size());
        return new TagSeedResult(tagNameToId, created);
    }

    public record TagData(String name, String description) {}

    public record TagSeedResult(Map<String, Long> tagNameToId, int count) {}
}