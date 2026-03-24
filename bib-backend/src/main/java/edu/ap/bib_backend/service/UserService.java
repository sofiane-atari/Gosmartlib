package edu.ap.bib_backend.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import edu.ap.bib_backend.dto.AuthUserDto;
import edu.ap.bib_backend.jpa.entity.RoleEntity;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import edu.ap.bib_backend.mapper.UserMapper;
import edu.ap.bib_backend.options.UserGetOptions;

@Service
public class UserService {

    private final ProfanityFilterService profanityFilterService;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository, ProfanityFilterService profanityFilterService, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.profanityFilterService = profanityFilterService;
        this.userMapper = userMapper;
    }

    public Optional<User> getUser(String userId) {
        return userRepository.findById(userId);
    }

    public List<AuthUserDto> getUsers(UserGetOptions options) {

        List<User> users;
        var optionsId = options.getWithId();
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
        return userRepository.save(user);
    }
}