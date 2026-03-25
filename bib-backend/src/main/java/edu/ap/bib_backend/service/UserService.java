package edu.ap.bib_backend.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import edu.ap.bib_backend.jpa.entity.RoleEntity;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> getUser(String userId) {
        return userRepository.findById(userId);
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