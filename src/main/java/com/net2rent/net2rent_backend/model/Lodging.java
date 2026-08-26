package com.net2rent.net2rent_backend.model;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "lodging", uniqueConstraints = @UniqueConstraint(columnNames = "ref"))
public class Lodging {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(nullable = false, unique = true)
    private String ref;

    @Column(nullable = false)
    private String pinHash; 

    @Column(nullable = false)
    private String name;

    private String address;

    @Column(columnDefinition = "TEXT")
    private String accessNotes;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

}
