package edu.ap.bib_backend.jpa.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "schools")
@Getter
@Setter
public class School {
    @Id
    private String domainName;
    private String name;
    private String adres; 
    private int postalCode;    
}