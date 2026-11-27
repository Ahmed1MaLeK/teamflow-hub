package com.teamflow.hub.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(nullable = false)
    public String name;
    @Column(nullable = false, unique = true, length = 50)
    public String username;
    @Column(nullable = false, unique = true)
    public String email;
    @Column(name = "password_hash", nullable = false)
    public String passwordHash;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    public Role role = Role.MEMBER;
    @Column(name = "created_at", nullable = false)
    public Instant createdAt = Instant.now();
}
