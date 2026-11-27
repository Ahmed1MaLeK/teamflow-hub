package com.teamflow.hub.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "user_id")
    public User user;
    @Column(nullable = false, unique = true)
    public String token;
    @Column(name = "expires_at", nullable = false)
    public Instant expiresAt;
    @Column(nullable = false)
    public boolean used = false;
}
