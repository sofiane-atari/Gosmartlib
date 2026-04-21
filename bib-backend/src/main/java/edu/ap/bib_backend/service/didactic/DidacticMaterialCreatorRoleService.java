package edu.ap.bib_backend.service.didactic;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ap.bib_backend.jpa.enums.Role;
import edu.ap.bib_backend.jpa.repository.UserRepository;

@Service
@Transactional(readOnly = true)
public class DidacticMaterialCreatorRoleService {

    private final UserRepository userRepository;

    public DidacticMaterialCreatorRoleService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Role getCreatorRole(String userId) {
        return userRepository.findRoleByUserId(userId).orElse(null);
    }
}