package com.teamflow.hub.service;

import com.teamflow.hub.dto.Dtos.NotificationDto;
import com.teamflow.hub.model.Notification;
import com.teamflow.hub.model.User;
import com.teamflow.hub.repo.NotificationRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {
    private final NotificationRepository repo;
    private final Mapper mapper;
    private final CurrentUser current;
    private final SimpMessagingTemplate ws;

    public NotificationService(NotificationRepository repo, Mapper mapper, CurrentUser current, SimpMessagingTemplate ws) {
        this.repo = repo;
        this.mapper = mapper;
        this.current = current;
        this.ws = ws;
    }

    @Transactional
    public void notify(User target, String message, String link) {
        if (target == null) return;
        Notification n = new Notification();
        n.user = target;
        n.message = message;
        n.link = link;
        repo.save(n);
        ws.convertAndSendToUser(target.email, "/queue/notifications", mapper.notification(n));
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> mine() {
        return repo.findByUserOrderByCreatedAtDesc(current.get()).stream().map(mapper::notification).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long unreadCount() { return repo.countByUserAndReadFalse(current.get()); }

    @Transactional
    public void markRead(Long id) {
        User me = current.get();
        repo.findById(id).filter(n -> n.user.id.equals(me.id)).ifPresent(n -> n.read = true);
    }

    @Transactional
    public void markAllRead() {
        repo.findByUserOrderByCreatedAtDesc(current.get()).forEach(n -> n.read = true);
    }
}
