package com.teamflow.hub.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "projects")
public class Project {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(name = "project_name", nullable = false)
    public String name;
    @Column(length = 4000)
    public String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    public ProjectStatus status = ProjectStatus.ACTIVE;
    @Column(name = "start_date")
    public LocalDate startDate;
    @Column(name = "end_date")
    public LocalDate endDate;
    @ManyToOne(optional = false) @JoinColumn(name = "manager_id")
    public User manager;
    @ManyToMany
    @JoinTable(name = "project_members", joinColumns = @JoinColumn(name = "project_id"), inverseJoinColumns = @JoinColumn(name = "user_id"))
    public Set<User> members = new HashSet<>();
    @ManyToMany
    @JoinTable(name = "project_teams", joinColumns = @JoinColumn(name = "project_id"), inverseJoinColumns = @JoinColumn(name = "team_id"))
    public Set<Team> teams = new HashSet<>();
    @Column(name = "created_at", nullable = false)
    public Instant createdAt = Instant.now();
}
