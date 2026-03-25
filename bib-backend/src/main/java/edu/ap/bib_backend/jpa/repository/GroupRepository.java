
package edu.ap.bib_backend.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.ap.bib_backend.jpa.entity.Group;

public interface GroupRepository extends JpaRepository<Group, String> {
}