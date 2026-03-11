package edu.ap.bib_backend.jpa.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Setter
@Getter
@Table(name = "users")
public class User {

    @Id
    private Long id;


    @OneToMany(mappedBy = "user")
    private List<BookLoan> loans= new ArrayList<>();
}