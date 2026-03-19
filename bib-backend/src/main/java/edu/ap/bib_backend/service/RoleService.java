package edu.ap.bib_backend.service;

import org.springframework.stereotype.Service;

import edu.ap.bib_backend.jpa.entity.Role;
import edu.ap.bib_backend.jpa.repository.UserRepository;

@Service
public class RoleService {

    private final UserRepository userRepository; 

    public RoleService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
   
    public Role getUserRole(String userId) {
        return userRepository.findById(userId)
                .map(user -> user.getRole().getRole())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}