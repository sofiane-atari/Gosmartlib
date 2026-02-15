package edu.ap.bib_backend.controller;

import edu.ap.bib_backend.jpa.entity.Test;
import edu.ap.bib_backend.jpa.repository.TestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = { "https://frontend.localhost", "https://gosmartlib.be", "http://localhost:4200" })
public class TestController {

    @Autowired
    private TestRepository repository;

    @GetMapping("/hello")
    public List<Test> getMessages() {
        return repository.findAll();
    }
}