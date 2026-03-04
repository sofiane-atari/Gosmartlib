package edu.ap.bib_backend.jpa.entity;

import jakarta.persistence.*;
import lombok.Setter;
import lombok.Getter;

@Entity
@Table(name = "books")
@Getter
@Setter
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "isbn_nummer")
    private Long ISBN;

    @Column(name = "titel")
    private String title;

    @Column(name = "auteur")
    private String author;

    @Column(name = "aantal_paginas")
    private int pages;

    @Column(name = "image_url")
    private String image_url;

    public Book() {
    }

    public Book(Long ISBN, String title, String author, int pages, String image_url) {
        this.ISBN = ISBN;
        this.title = title;
        this.author = author;
        this.pages = pages;
        this.image_url = image_url;
    }
}