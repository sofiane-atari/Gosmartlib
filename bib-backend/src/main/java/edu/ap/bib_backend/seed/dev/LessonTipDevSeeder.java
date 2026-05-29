package edu.ap.bib_backend.seed.dev;

import edu.ap.bib_backend.dto.lessontips.CreateLessonTipRequestDto;
import edu.ap.bib_backend.dto.lessontips.LessonTipResponseDto;
import edu.ap.bib_backend.jpa.enums.LessonTipType;
import edu.ap.bib_backend.service.lessontips.LessonTipService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class LessonTipDevSeeder {

    private final LessonTipService lessonTipService;

    public LessonTipDevSeeder(LessonTipService lessonTipService) {
        this.lessonTipService = lessonTipService;
    }

    private static final List<LessonTipSeedData> LESSON_TIP_DATA = List.of(
            new LessonTipSeedData(
                    "Introductie tot programmeren met Python",
                    "Een lesopzet om studenten kennis te laten maken met de basisconcepten van programmeren via Python. " +
                            "Bevat oefeningen over variabelen, loops en functies.",
                    LessonTipType.LESSON_METHOD,
                    "Informatica",
                    "2e graad",
                    "python, programmeren, loops, variabelen"
            ),
            new LessonTipSeedData(
                    "Objectgeoriënteerd ontwerpen in Java",
                    "Didactische aanpak om de principes van OO-programmeren (overerving, polymorfisme, encapsulatie) " +
                            "uit te leggen met praktische voorbeelden.",
                    LessonTipType.DIDACTIC_HANDBOOK,
                    "Informatica",
                    "3e graad",
                    "java, OO, overerving, polymorfisme"
            ),
            new LessonTipSeedData(
                    "Webontwikkeling: front-end basis",
                    "Lesmateriaal voor het bouwen van een eerste webpagina met HTML, CSS en JavaScript. " +
                            "Inclusief opdrachten en evaluatiecriteria.",
                    LessonTipType.WORKSHEET,
                    "Informatica",
                    "2e graad",
                    "html, css, javascript, webdevelopment"
            ),
            new LessonTipSeedData(
                    "Algoritmen visualiseren",
                    "Werkvorm om sorteeralgoritmen visueel voor te stellen. Studenten implementeren bubble sort, " +
                            "insertion sort en quicksort en vergelijken de prestaties.",
                    LessonTipType.LESSON_IDEA,
                    "Informatica",
                    "3e graad",
                    "algoritmen, sorteren, visualisatie"
            ),
            new LessonTipSeedData(
                    "Beveiliging in netwerken",
                    "Lessenserie over cyberbeveiliging metcases uit de praktijk. Studenten leren over encryptie, " +
                            "netwerkprotocollen en ethisch hacken.",
                    LessonTipType.EVALUATION_MATERIAL,
                    "Informatica",
                    "3e graad",
                    "cyberbeveiliging, encryptie, netwerken"
            ),
            new LessonTipSeedData(
                    "Refactoring: codekwaliteit verbeteren",
                    "Oefeningenmap waarin studenten bestaande code refactoren volgens de principes uit Clean Code. " +
                            "Met zelfevaluatiekaarten.",
                    LessonTipType.WORKSHEET,
                    "Informatica",
                    "3e graad",
                    "refactoring, clean code, codekwaliteit"
            ),
            new LessonTipSeedData(
                    "Presentatie over design patterns",
                    "Klaar-om-te-gebruiken presentatie over de belangrijkste design patterns (Singleton, Observer, " +
                            "Strategy, Factory) met voorbeelden in Java.",
                    LessonTipType.PRESENTATION,
                    "Informatica",
                    "3e graad",
                    "design patterns, observer, strategy, factory"
            ),
            new LessonTipSeedData(
                    "Microservices architectuur bespreken",
                    "Discussiegids en referentiemateriaal voor een les over microservices, " +
                            " inclusief voordelen, nadelen en wanneer je ze wel/niet moet gebruiken.",
                    LessonTipType.REFERENCE_WORK,
                    "Informatica",
                    "3e graad",
                    "microservices, architectuur, distributed systems"
            )
    );

    public LessonTipSeedResult seed(String teacherId, List<Long> bookIds) {
        if (bookIds.isEmpty()) {
            log.info("Geen boeken gevonden, lestip seeding overgeslagen");
            return new LessonTipSeedResult(0, List.of());
        }

        int created = 0;
        List<Long> createdTipIds = new ArrayList<>();

        for (int i = 0; i < LESSON_TIP_DATA.size(); i++) {
            LessonTipSeedData data = LESSON_TIP_DATA.get(i);

            int bookCount = Math.min(2 + (i % 3), bookIds.size());
            List<Long> linkedBookIds = bookIds.subList(0, bookCount);

            CreateLessonTipRequestDto request = new CreateLessonTipRequestDto(
                    data.title,
                    data.description,
                    data.lessonTipType,
                    data.subject,
                    data.educationLevel,
                    data.keywords,
                    linkedBookIds
            );

            try {
                LessonTipResponseDto dto = lessonTipService.createLessonTip(request, teacherId);
                created++;
                createdTipIds.add(dto.id());
                log.info("Lestip aangemaakt: {}", data.title);
            } catch (Exception e) {
                log.warn("Kon lestip niet aanmaken '{}': {}", data.title, e.getMessage());
            }
        }

        log.info("Lestip seeding voltooid: {} van {} lestips aangemaakt", created, LESSON_TIP_DATA.size());
        return new LessonTipSeedResult(created, createdTipIds);
    }

    public record LessonTipSeedData(String title, String description, LessonTipType lessonTipType,
                                     String subject, String educationLevel, String keywords) {}

    public record LessonTipSeedResult(int count, List<Long> lessonTipIds) {}
}