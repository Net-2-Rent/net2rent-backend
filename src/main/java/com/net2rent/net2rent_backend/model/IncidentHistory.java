package com.net2rent.net2rent_backend.model;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "incident_history")
public class IncidentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private AppUser actor; 

    @Column(nullable = false)
    private String eventType; 

    private String previousValue;
    private String newValue;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

}