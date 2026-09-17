package com.net2rent.net2rent_backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "account")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL)
    private List<AppUser> users;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL)
    private List<Lodging> lodgings;

}
