package edu.ap.bib_backend.jpa.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
    private String id;


    @OneToMany(mappedBy = "user")
    private List<BookLoan> loans= new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "role_id")
    private RoleEntity role;
}