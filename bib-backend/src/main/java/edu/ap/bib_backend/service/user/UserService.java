package edu.ap.bib_backend.service.user;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ap.bib_backend.dto.AuthUserDto;
import edu.ap.bib_backend.exception.ResourceNotFoundException;
import edu.ap.bib_backend.jpa.entity.Campus;
import edu.ap.bib_backend.jpa.entity.RoleEntity;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.repository.CampusRepository;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import edu.ap.bib_backend.mapper.UserMapper;
import edu.ap.bib_backend.options.UserGetOptions;
import edu.ap.bib_backend.service.GroupService;
import edu.ap.bib_backend.service.review.ProfanityFilterService;

@Service
public class UserService {

    private final ProfanityFilterService profanityFilterService;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final GroupService groupService;
    private final CampusRepository campusRepository;
    private final CurrentUserService currentUserService;

    public UserService(
            UserRepository userRepository,
            ProfanityFilterService profanityFilterService,
            UserMapper userMapper,
            GroupService groupService,
            CampusRepository campusRepository,
            CurrentUserService currentUserService
    ) {
        this.userRepository = userRepository;
        this.profanityFilterService = profanityFilterService;
        this.userMapper = userMapper;
        this.groupService = groupService;
        this.campusRepository = campusRepository;
        this.currentUserService = currentUserService;
    }

    public Optional<User> getUser(String userId) {
        return userRepository.findById(userId);
    }

    public List<AuthUserDto> getUsers(UserGetOptions options) {
        List<User> users;
        var optionsId = options.getId();

        if (!optionsId.isBlank()) {
            users = userRepository.findByIdContaining(optionsId);
        } else {
            users = userRepository.findAll();
        }

        return userMapper.toDtoList(users);
    }

    public User createUser(String userId, RoleEntity role, School school) {
        if (userRepository.existsById(userId)) {
            throw new RuntimeException("Gebruiker bestaat al");
        }

        School resolvedSchool = Objects.requireNonNull(school, "school mag niet null zijn");
        Campus defaultCampus = campusRepository.findBySchoolIdAndDefaultCampusTrue(resolvedSchool.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "School heeft geen default-campus: " + resolvedSchool.getId()
                ));

        User user = new User();
        user.setId(userId);
        user.setRole(role);
        user.setSchool(resolvedSchool);
        user.setCampus(defaultCampus);

        return userRepository.save(user);
    }

    @Transactional
    public AuthUserDto switchCurrentUserCampus(Authentication authentication, Long campusId) {
        if (campusId == null) {
            throw new IllegalArgumentException("campusId is verplicht.");
        }

        String currentUserId = currentUserService.extractCurrentUserId(authentication);

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "userId",
                        "Gebruiker niet gevonden met id: " + currentUserId
                ));

        if (user.getSchool() == null) {
            throw new IllegalStateException("Gebruiker heeft geen school gekoppeld.");
        }

        Campus campus = campusRepository.findById(campusId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "campusId",
                        "Campus niet gevonden met id: " + campusId
                ));

        if (campus.getSchool() == null || !campus.getSchool().getId().equals(user.getSchool().getId())) {
            throw new IllegalArgumentException("Deze campus hoort niet bij jouw school.");
        }

        user.setCampus(campus);

        return userMapper.toDto(userRepository.save(user));
    }

    public AuthUserDto setReviewBlocked(String userId, boolean blocked) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "userId",
                        "Gebruiker niet gevonden met id: " + userId
                ));

        user.setReviewBlocked(blocked);
        User updatedUser = userRepository.save(user);
        return userMapper.toDto(updatedUser);
    }

    public User save(User user) {
        if (user.getSchool() != null && user.getCampus() == null) {
            Campus defaultCampus = campusRepository.findBySchoolIdAndDefaultCampusTrue(user.getSchool().getId())
                    .orElseThrow(() -> new IllegalStateException(
                            "School heeft geen default-campus: " + user.getSchool().getId()
                    ));

            user.setCampus(defaultCampus);
        }

        return userRepository.save(user);
    }
}