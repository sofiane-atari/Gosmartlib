package edu.ap.bib_backend.jpa.repository.didactic;


import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import edu.ap.bib_backend.jpa.entity.didactic.DidacticMaterial;

import java.util.List;
import java.util.Optional;

public interface DidacticMaterialRepository extends JpaRepository<DidacticMaterial, Long> {

    @Override
    @EntityGraph(attributePaths = {"bookLinks", "bookLinks.book"})
    List<DidacticMaterial> findAll();

    @Override
    @EntityGraph(attributePaths = {"bookLinks", "bookLinks.book"})
    Optional<DidacticMaterial> findById(Long id);
}