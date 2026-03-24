package edu.ap.bib_backend.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import edu.ap.bib_backend.jpa.entity.RoleEntity;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private static final String FALLBACK_CLASS_CODE = "GENERAL";

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    public Optional<User> getUser(String userId) {
        return userRepository.findById(userId);
    }

    public User createUser(String userId, RoleEntity role) {
        return createUser(userId, role, FALLBACK_CLASS_CODE);
    }

    public User createUser(String userId, RoleEntity role, String classCode) {
        if (userRepository.existsById(userId)) {
            throw new RuntimeException("Gebruiker bestaat al");
        }
        User user = new User();
        user.setId(userId);
        user.setRole(role);
        user.setClassCode(normalizeClassCode(classCode));
        return userRepository.save(user);
    }

    public User saveWithClassCodeIfChanged(User user, String classCode) {
        String normalizedClassCode = normalizeClassCode(classCode);
        if (normalizedClassCode.equals(user.getClassCode())) {
            return user;
        }

        user.setClassCode(normalizedClassCode);
        return userRepository.save(user);
    }

    private String normalizeClassCode(String classCode) {
        if (classCode == null || classCode.isBlank()) {
            return FALLBACK_CLASS_CODE;
        }
        return classCode.trim();
    }

}