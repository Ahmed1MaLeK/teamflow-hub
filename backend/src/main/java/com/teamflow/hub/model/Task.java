package com.teamflow.hub.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "tasks")
public class Task {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(nullable = false)
    public String title;
    @Column(length = 4000)
    public String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    public TaskStatus status = TaskStatus.PENDING;
    @Column(name = "start_date")
    public LocalDate startDate;
    public LocalDate deadline;
    @ManyToOne(optional = false) @JoinColumn(name = "project_id")
    public Project project;
    @ManyToOne @JoinColumn(name = "assigned_to")
    public User assignedTo;
    @ManyToOne(optional = false) @JoinColumn(name = "created_by")
    public User createdBy;
    /** Quality rating 1-5 given by the manager when the task is accepted. */
    public Integer rating;
    @Column(name = "review_feedback", length = 2000)
    public String reviewFeedback;
    @Column(name = "created_at", nullable = false)
    public Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt = Instant.now();
}
