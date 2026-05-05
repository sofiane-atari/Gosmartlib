package edu.ap.bib_backend.jpa.entity;

import java.util.ArrayList;
import java.util.List;


import edu.ap.bib_backend.jpa.entity.loan.Loan;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Setter
@Getter
@Table(name = "users")
public class User {

    @Id
    private String id;

    @Column(name = "oneroster_sourced_id")
    private String oneRosterSourcedId;

    @OneToMany(mappedBy = "user")
    private List<Loan> loans = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "role_id", nullable = false)
    private RoleEntity role;

    @ManyToOne
    @JoinColumn(name = "group_name")
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campus_id", nullable = false)
    private Campus campus;

    @Column(name = "review_blocked", nullable = false)
    private boolean reviewBlocked = false;
}