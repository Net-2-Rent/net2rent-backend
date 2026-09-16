package com.net2rent.net2rent_backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor 
@AllArgsConstructor 
@Builder 
@Entity 
@Table(name = "incident_time_entry")
public class IncidentTimeEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private AppUser author;

    @Column(nullable = false, length = 100)
    private String concept;

    @Column(nullable = false)
    private Integer minutes;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

}
