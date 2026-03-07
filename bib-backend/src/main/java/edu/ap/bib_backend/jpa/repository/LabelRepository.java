package edu.ap.bib_backend.jpa.repository;

import edu.ap.bib_backend.jpa.entity.Label;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LabelRepository extends JpaRepository<Label, String> {
}