package edu.ap.bib_backend.jpa.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.ap.bib_backend.jpa.entity.RoleEntity;
import edu.ap.bib_backend.jpa.enums.Role;

public interface RoleEntityRepository extends JpaRepository<RoleEntity, Role> {
    Optional<RoleEntity> findByRole(Role role);
}