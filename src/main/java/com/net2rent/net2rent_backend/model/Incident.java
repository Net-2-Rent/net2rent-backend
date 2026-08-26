package com.net2rent.net2rent_backend.model;

import java.time.LocalDateTime;

import com.net2rent.net2rent_backend.model.enums.*;
import jakarta.persistence.*;
import lombok.*;
import java.util.List;



@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "incident", uniqueConstraints = @UniqueConstraint(columnNames = "code"))
public class Incident {

   @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(nullable = false, unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IncidentSource source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IncidentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IncidentPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private IncidentCategory category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lodging_id", nullable = false)
    private Lodging lodging;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    // Datos del huésped, embebidos.
    @Column(nullable = false)
    private String guestFirstName;

    @Column(nullable = false)
    private String guestLastName;

    private String guestContact;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private AppUser assignee; 

    @Column(nullable = false)
    private LocalDateTime openedAt; 

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt; 

    private LocalDateTime assignedAt;
    private LocalDateTime startedAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime closedAt;

    private Integer minutesSpent;

    @Column(columnDefinition = "TEXT")
    private String resolutionNote;

    @Column(columnDefinition = "TEXT")
    private String pauseReason;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(columnDefinition = "TEXT")
    private String reassignmentReason;

    @OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<IncidentCheckListItem> checklistItems;

    @OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<IncidentComment> comments;

    @OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<IncidentHistory> history; 

}
