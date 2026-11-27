package com.teamflow.hub.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "chat_messages", indexes = {
        @Index(name = "idx_chat_project", columnList = "project_id, channel"),
        @Index(name = "idx_chat_direct", columnList = "sender_id, receiver_id")})
public class ChatMessage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    public ChannelType channel;
    @ManyToOne(optional = false) @JoinColumn(name = "sender_id")
    public User sender;
    /** Only set for DIRECT messages. */
    @ManyToOne @JoinColumn(name = "receiver_id")
    public User receiver;
    /** Only set for PROJECT and ANNOUNCEMENT messages. */
    @ManyToOne @JoinColumn(name = "project_id")
    public Project project;
    @Column(name = "message_text", nullable = false, length = 4000)
    public String text;
    @Column(name = "sent_at", nullable = false)
    public Instant sentAt = Instant.now();
}
