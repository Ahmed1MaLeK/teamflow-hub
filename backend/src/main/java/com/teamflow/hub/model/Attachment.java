package com.teamflow.hub.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "attachments")
public class Attachment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @ManyToOne @JoinColumn(name = "project_id")
    public Project project;
    @ManyToOne @JoinColumn(name = "task_id")
    public Task task;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    public AttachmentKind kind = AttachmentKind.PROJECT_FILE;
    @Column(name = "file_name", nullable = false)
    public String fileName;
    @Column(name = "stored_name", nullable = false)
    public String storedName;
    @ManyToOne(optional = false) @JoinColumn(name = "uploaded_by")
    public User uploadedBy;
    @Column(name = "created_at", nullable = false)
    public Instant createdAt = Instant.now();
}
