package com.teamflow.hub.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "notifications")
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "user_id")
    public User user;
    @Column(nullable = false, length = 1000)
    public String message;
    public String link;
    @Column(name = "is_read", nullable = false)
    public boolean read = false;
    @Column(name = "created_at", nullable = false)
    public Instant createdAt = Instant.now();
}
