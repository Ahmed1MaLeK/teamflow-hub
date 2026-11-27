package com.teamflow.hub.web;

import com.teamflow.hub.dto.Dtos.*;
import com.teamflow.hub.model.ChannelType;
import com.teamflow.hub.service.ChatService;
import com.teamflow.hub.service.DashboardService;
import com.teamflow.hub.service.NotificationService;
import com.teamflow.hub.service.TeamService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class MiscController {
    private final TeamService teams;
    private final ChatService chat;
    private final NotificationService notifications;
    private final DashboardService dashboard;

    public MiscController(TeamService teams, ChatService chat, NotificationService notifications, DashboardService dashboard) {
        this.teams = teams;
        this.chat = chat;
        this.notifications = notifications;
        this.dashboard = dashboard;
    }

    // ----- teams & users -----
    @GetMapping("/teams") public List<TeamDto> teams() { return teams.list(); }

    @PostMapping("/teams") @ResponseStatus(HttpStatus.CREATED)
    public TeamDto createTeam(@Valid @RequestBody TeamRequest r) { return teams.create(r); }

    @PutMapping("/teams/{id}")
    public TeamDto updateTeam(@PathVariable Long id, @Valid @RequestBody TeamRequest r) { return teams.update(id, r); }

    @DeleteMapping("/teams/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTeam(@PathVariable Long id) { teams.delete(id); }

    @GetMapping("/users") public List<UserDto> users(@RequestParam(required = false) String q) { return teams.searchUsers(q); }

    // ----- chat -----
    @GetMapping("/chat/channels") public ChannelsDto channels() { return chat.channels(); }

    @GetMapping("/chat/messages")
    public List<ChatMessageDto> history(@RequestParam ChannelType channel, @RequestParam(required = false) Long projectId,
                                        @RequestParam(required = false) Long userId) {
        return chat.history(channel, projectId, userId);
    }

    @PostMapping("/chat/messages") @ResponseStatus(HttpStatus.CREATED)
    public ChatMessageDto send(@Valid @RequestBody SendMessageRequest r) { return chat.send(r); }

    // ----- notifications -----
    @GetMapping("/notifications") public List<NotificationDto> notifications() { return notifications.mine(); }
    @GetMapping("/notifications/unread-count") public Map<String, Long> unread() { return Map.of("count", notifications.unreadCount()); }

    @PostMapping("/notifications/{id}/read") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void read(@PathVariable Long id) { notifications.markRead(id); }

    @PostMapping("/notifications/read-all") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void readAll() { notifications.markAllRead(); }

    // ----- dashboard -----
    @GetMapping("/dashboard") public DashboardDto dashboard() { return dashboard.dashboard(); }
    @GetMapping("/insights") public InsightsDto insights() { return dashboard.insights(); }
}
