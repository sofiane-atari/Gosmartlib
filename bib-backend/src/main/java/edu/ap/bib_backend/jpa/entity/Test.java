package edu.ap.bib_backend.jpa.entity;

import jakarta.persistence.*;
import lombok.Setter;
import lombok.Getter;


@Entity
@Table(name = "test_message")
@Getter
@Setter
public class Test {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String message;
}
