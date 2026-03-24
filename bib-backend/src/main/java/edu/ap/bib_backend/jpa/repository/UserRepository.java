package edu.ap.bib_backend.jpa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.ap.bib_backend.jpa.entity.User;

public interface UserRepository extends JpaRepository<User, String>{
    public List<User> findByIdContaining(String Id);
}