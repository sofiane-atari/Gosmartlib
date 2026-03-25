package edu.ap.bib_backend.jpa.entity;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "classes")
@Getter
@Setter
public class Group {

    @Id
    private String name;

    @OneToMany(mappedBy = "group")
    private List<User> users;
}