package com.teamflow.hub.service;

import com.teamflow.hub.dto.Dtos.*;
import com.teamflow.hub.model.*;
import com.teamflow.hub.repo.ChatMessageRepository;
import com.teamflow.hub.repo.ProjectRepository;
import com.teamflow.hub.repo.UserRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Channels: DIRECT (user to user), PROJECT (group chat of a project, everyone in it can write),
 * ANNOUNCEMENT (project announcements, only the project manager can write).
 * Messages are saved over REST and pushed live over STOMP/WebSocket.
 */
@Service
public class ChatService {
    private final ChatMessageRepository messages;
    private final ProjectRepository projects;
    private final UserRepository users;
    private final ProjectService projectService;
    private final CurrentUser current;
    private final Mapper mapper;
    private final SimpMessagingTemplate ws;
    private final NotificationService notifications;

    public ChatService(ChatMessageRepository messages, ProjectRepository projects, UserRepository users,
                       ProjectService projectService, CurrentUser current, Mapper mapper, SimpMessagingTemplate ws,
                       NotificationService notifications) {
        this.messages = messages;
        this.projects = projects;
        this.users = users;
        this.projectService = projectService;
        this.current = current;
        this.mapper = mapper;
        this.ws = ws;
        this.notifications = notifications;
    }

    public static String topic(Long projectId, ChannelType c) { return "/topic/project." + projectId + "." + c.name().toLowerCase(); }

    @Transactional(readOnly = true)
    public ChannelsDto channels() {
        User me = current.get();
        List<ProjectRef> refs = projects.findVisibleTo(me).stream().map(p -> new ProjectRef(p.id, p.name)).collect(Collectors.toList());
        Set<User> people = new HashSet<>();
        for (Project p : projects.findVisibleTo(me)) {
            people.add(p.manager);
            people.addAll(p.members);
        }
        people.removeIf(u -> u.id.equals(me.id));
        List<UserDto> directs = people.stream().map(UserDto::of).sorted((a, b) -> a.name().compareToIgnoreCase(b.name())).collect(Collectors.toList());
        return new ChannelsDto(directs, refs);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDto> history(ChannelType channel, Long projectId, Long userId) {
        User me = current.get();
        List<ChatMessage> found;
        if (channel == ChannelType.DIRECT) {
            if (userId == null) throw ApiException.badRequest("userId is required");
            User other = users.findById(userId).orElseThrow(() -> ApiException.notFound("User"));
            found = messages.findConversation(me, other);
        } else {
            if (projectId == null) throw ApiException.badRequest("projectId is required");
            Project p = projectService.loadVisible(projectId, me);
            found = messages.findByProjectAndChannelOrderBySentAtAsc(p, channel);
        }
        return found.stream().map(mapper::message).collect(Collectors.toList());
    }

    @Transactional
    public ChatMessageDto send(SendMessageRequest r) {
        User me = current.get();
        ChatMessage m = new ChatMessage();
        m.sender = me;
        m.channel = r.channel();
        m.text = r.text().trim();
        if (m.text.isEmpty()) throw ApiException.badRequest("Message is empty");

        if (r.channel() == ChannelType.DIRECT) {
            if (r.receiverId() == null) throw ApiException.badRequest("receiverId is required");
            if (r.receiverId().equals(me.id)) throw ApiException.badRequest("You cannot message yourself");
            m.receiver = users.findById(r.receiverId()).orElseThrow(() -> ApiException.notFound("User"));
            messages.save(m);
            ChatMessageDto dto = mapper.message(m);
            ws.convertAndSendToUser(m.receiver.email, "/queue/direct", dto);
            ws.convertAndSendToUser(me.email, "/queue/direct", dto);
            notifications.notify(m.receiver, "New message from " + me.name, "/messages");
            return dto;
        }
        if (r.projectId() == null) throw ApiException.badRequest("projectId is required");
        Project p = projectService.loadVisible(r.projectId(), me);
        if (r.channel() == ChannelType.ANNOUNCEMENT && !p.manager.id.equals(me.id)) {
            throw ApiException.forbidden("Only the project manager can post announcements");
        }
        m.project = p;
        messages.save(m);
        ChatMessageDto dto = mapper.message(m);
        ws.convertAndSend(topic(p.id, r.channel()), dto);
        if (r.channel() == ChannelType.ANNOUNCEMENT) {
            for (User u : p.members) notifications.notify(u, "Announcement in " + p.name, "/messages");
        }
        return dto;
    }
}
