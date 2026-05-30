package edu.ap.bib_backend.seed.dev;

import edu.ap.bib_backend.jpa.entity.Campus;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.enums.Role;
import edu.ap.bib_backend.jpa.repository.CampusRepository;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import edu.ap.bib_backend.service.user.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DevSeedService {

    @Value("${app.dev-mode-enabled:false}")
    private boolean devModeEnabled;

    private final TagDevSeeder tagDevSeeder;
    private final BookDevSeeder bookDevSeeder;
    private final LoanDevSeeder loanDevSeeder;
    private final ReviewDevSeeder reviewDevSeeder;
    private final ReadingListDevSeeder readingListDevSeeder;
    private final LessonTipDevSeeder lessonTipDevSeeder;
    private final MailSubscriptionDevSeeder mailSubscriptionDevSeeder;
    private final DevSeedCleanupService cleanupService;
    private final UserRepository userRepository;
    private final CampusRepository campusRepository;
    private final CurrentUserService currentUserService;

    private SeedTrack lastSeed = null;

    private record SeedTrack(
            List<Long> bookIds,
            Map<String, Long> tagNameToId,
            List<Long> readingListIds,
            List<Long> lessonTipIds,
            List<MailSubscriptionDevSeeder.SubscriptionKey> mailSubscriptions,
            Long schoolId
    ) {}

    public DevSeedResult seed(Authentication authentication) {
        if (!devModeEnabled) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Dev seed is alleen beschikbaar in development-modus.");
        }

        log.info("=== Dev seed gestart ===");

        User currentUser = currentUserService.getCurrentUser(authentication);
        Long schoolId = currentUser.getSchool().getId();

        Long campusId = campusRepository.findFirstBySchoolIdOrderByIdAsc(schoolId)
                .map(Campus::getId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "School heeft geen campussen."));

        List<User> schoolUsers = userRepository.findBySchoolId(schoolId);
        List<String> studentIds = schoolUsers.stream()
                .filter(u -> u.getRole() != null && u.getRole().getRole() == Role.STUDENT)
                .map(User::getId)
                .collect(Collectors.toList());

        List<String> teacherIds = schoolUsers.stream()
                .filter(u -> u.getRole() != null && u.getRole().getRole() == Role.TEACHER)
                .map(User::getId)
                .collect(Collectors.toList());

        log.info("School {}: {} studenten, {} leerkrachten gevonden",
                schoolId, studentIds.size(), teacherIds.size());

        if (studentIds.isEmpty()) {
            log.info("Geen studenten gevonden, sommige seeders worden overgeslagen");
        }
        if (teacherIds.isEmpty()) {
            log.info("Geen leerkrachten gevonden, lestip seeding wordt overgeslagen");
        }

        DevSeedResult totalResult = new DevSeedResult(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

        log.info("--- Tags aanmaken ---");
        TagDevSeeder.TagSeedResult tagResult = tagDevSeeder.seed(authentication);
        totalResult = totalResult.add(new DevSeedResult(tagResult.count(), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));

        log.info("--- Boeken aanmaken ---");
        BookDevSeeder.BookSeedResult bookResult = bookDevSeeder.seed(
                authentication, campusId, tagResult.tagNameToId());
        totalResult = totalResult.add(new DevSeedResult(0, bookResult.booksCreated(),
                bookResult.copiesCreated(), 0, 0, 0, 0, 0, 0, 0, 0));

        List<Long> bookIds = bookResult.bookIds();

        log.info("--- Leningen aanmaken ---");
        LoanDevSeeder.LoanSeedResult loanResult = loanDevSeeder.seed(
                authentication, schoolId, campusId, bookIds, studentIds);
        totalResult = totalResult.add(new DevSeedResult(0, 0, 0,
                loanResult.loansCreated(), loanResult.loansReturned(), 0, 0, 0, 0, 0, 0));

        log.info("--- Reviews aanmaken ---");
        ReviewDevSeeder.ReviewSeedResult reviewResult = reviewDevSeeder.seed(bookIds, studentIds);
        totalResult = totalResult.add(new DevSeedResult(0, 0, 0, 0, 0,
                reviewResult.reviewsCreated(), reviewResult.reviewsReported(), 0, 0, 0, 0));

        log.info("--- Leeslijsten aanmaken ---");
        int totalListsCreated = 0;
        int totalFavoritesAdded = 0;
        List<Long> allReadingListIds = new ArrayList<>();
        int maxStudentsForLists = Math.min(5, studentIds.size());
        for (int i = 0; i < maxStudentsForLists; i++) {
            String studentId = studentIds.get(i);
            ReadingListDevSeeder.ReadingListSeedResult rlResult =
                    readingListDevSeeder.seed(studentId, bookIds);
            totalListsCreated += rlResult.listsCreated();
            totalFavoritesAdded += rlResult.favoritesAdded();
            allReadingListIds.addAll(rlResult.readingListIds());
        }
        totalResult = totalResult.add(new DevSeedResult(0, 0, 0, 0, 0, 0, 0,
                totalListsCreated, totalFavoritesAdded, 0, 0));

        log.info("--- Lestips aanmaken ---");
        int totalLessonTips = 0;
        List<Long> allLessonTipIds = new ArrayList<>();
        if (!teacherIds.isEmpty()) {
            String teacherId = teacherIds.get(0);
            LessonTipDevSeeder.LessonTipSeedResult ltResult =
                    lessonTipDevSeeder.seed(teacherId, bookIds);
            totalLessonTips = ltResult.count();
            allLessonTipIds.addAll(ltResult.lessonTipIds());

            if (teacherIds.size() > 1) {
                String secondTeacherId = teacherIds.get(1);
                LessonTipDevSeeder.LessonTipSeedResult ltResult2 =
                        lessonTipDevSeeder.seed(secondTeacherId, bookIds.subList(
                                0, Math.min(4, bookIds.size())));
                totalLessonTips += ltResult2.count();
                allLessonTipIds.addAll(ltResult2.lessonTipIds());
            }
        }
        totalResult = totalResult.add(new DevSeedResult(0, 0, 0, 0, 0, 0, 0, 0, 0, totalLessonTips, 0));

        log.info("--- Mailabonnementen aanmaken ---");
        MailSubscriptionDevSeeder.MailSubscriptionSeedResult msResult =
                mailSubscriptionDevSeeder.seed(studentIds, bookIds);
        totalResult = totalResult.add(new DevSeedResult(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, msResult.count()));

        lastSeed = new SeedTrack(
                new ArrayList<>(bookResult.bookIds()),
                new HashMap<>(tagResult.tagNameToId()),
                new ArrayList<>(allReadingListIds),
                new ArrayList<>(allLessonTipIds),
                new ArrayList<>(msResult.subscriptions()),
                schoolId
        );

        log.info("=== Dev seed voltooid ===: tags={}, boeken={}, exemplaren={}, leningen={}, " +
                        "geretourneerd={}, reviews={}, rapportages={}, leeslijsten={}, favorieten={}, lestips={}, abonnementen={}",
                totalResult.tagsCreated(), totalResult.booksCreated(), totalResult.copiesCreated(),
                totalResult.loansCreated(), totalResult.loansReturned(), totalResult.reviewsCreated(),
                totalResult.reviewsReported(), totalResult.readingListsCreated(), totalResult.favoritesAdded(),
                totalResult.lessonTipsCreated(), totalResult.mailSubscriptionsCreated());

        return totalResult;
    }

    public void clear(Authentication authentication) {
        if (!devModeEnabled) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Dev seed is alleen beschikbaar in development-modus.");
        }

        User currentUser = currentUserService.getCurrentUser(authentication);
        Long schoolId = currentUser.getSchool().getId();

        if (lastSeed == null) {
            throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED,
                    "Geen seed-gegevens gevonden om te wissen. Voer eerst een dev-seed uit.");
        }

        if (!lastSeed.schoolId().equals(schoolId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Seed-gegevens behoren tot een andere school. Voer eerst een dev-seed uit voor deze school.");
        }

        log.info("=== Dev seed cleanup gestart voor school {} ===", schoolId);

        cleanupService.clear(
                lastSeed.bookIds(),
                lastSeed.tagNameToId(),
                lastSeed.readingListIds(),
                lastSeed.lessonTipIds(),
                lastSeed.mailSubscriptions()
        );

        lastSeed = null;

        log.info("=== Dev seed cleanup voltooid voor school {} ===", schoolId);
    }
}