package edu.ap.bib_backend.service.user;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import edu.ap.bib_backend.dto.AuthUserDto;
import edu.ap.bib_backend.exception.ResourceNotFoundException;
import edu.ap.bib_backend.jpa.entity.RoleEntity;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import edu.ap.bib_backend.mapper.UserMapper;
import edu.ap.bib_backend.options.UserGetOptions;
import edu.ap.bib_backend.service.GroupService;
import edu.ap.bib_backend.service.ProfanityFilterService;

@Service
public class UserService {

    private final ProfanityFilterService profanityFilterService;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final GroupService groupService;
    private final SchoolRepository schoolRepository;

    public UserService(UserRepository userRepository, ProfanityFilterService profanityFilterService,
            UserMapper userMapper, GroupService groupService, SchoolRepository schoolRepository) {
        this.userRepository = userRepository;
        this.profanityFilterService = profanityFilterService;
        this.userMapper = userMapper;
        this.groupService = groupService;
        this.schoolRepository = schoolRepository;
    }

    public Optional<User> getUser(String userId) {
        return userRepository.findById(userId);
    }

    public List<AuthUserDto> getUsers(UserGetOptions options) {
        List<User> users;
        var optionsId = options.getId();

        if (!optionsId.isBlank())
            users = userRepository.findByIdContaining(optionsId);
        else
            users = userRepository.findAll();

        var dtos = userMapper.toDtoList(users);

        return dtos;
    }

    public User createUser(String userId, RoleEntity role) {
        if (userRepository.existsById(userId)) {
            throw new RuntimeException("Gebruiker bestaat al");
        }
        User user = new User();
        user.setId(userId);
        user.setRole(role);

        School school = schoolRepository.findById(1L)
                .orElseThrow(() -> new IllegalStateException("Default school not found"));

        user.setSchool(school);

        return userRepository.save(user);
    }

    public AuthUserDto setReviewBlocked(String userId, boolean blocked) {
        User user = userRepository.findById(userId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("userId", "Gebruiker niet gevonden met id: " + userId));

        user.setReviewBlocked(blocked);
        User updatedUser = userRepository.save(user);
        return userMapper.toDto(updatedUser);
    }
}