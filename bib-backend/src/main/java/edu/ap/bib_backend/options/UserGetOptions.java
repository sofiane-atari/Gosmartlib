package edu.ap.bib_backend.options;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UserGetOptions {
    private String firstName = "";
    private String lastName = "";
    private String id = "";
}