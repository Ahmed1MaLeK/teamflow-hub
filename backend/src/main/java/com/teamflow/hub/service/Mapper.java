package com.teamflow.hub.service;

import com.teamflow.hub.dto.Dtos.*;
import com.teamflow.hub.model.*;
import com.teamflow.hub.repo.TaskRepository;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/** Entity to DTO mapping. Call inside a transaction (lazy collections are read here). */
@Component
public class Mapper {
    private final TaskRepository tasks;

    public Mapper(TaskRepository tasks) { this.tasks = tasks; }

    public TeamDto team(Team t) {
        return new TeamDto(t.id, t.name, t.members.stream().sorted(Comparator.comparing(u -> u.name))
                .map(UserDto::of).collect(Collectors.toList()));
    }

    public ProjectDto project(Project p) {
        List<Task> all = tasks.findByProjectOrderByCreatedAtDesc(p);
        int total = all.size();
        int done = (int) all.stream().filter(t -> isComplete(t.status)).count();
        int progress = total == 0 ? 0 : Math.round(done * 100f / total);
        return new ProjectDto(p.id, p.name, p.description, p.status, p.startDate, p.endDate, UserDto.of(p.manager),
                p.members.stream().sorted(Comparator.comparing(u -> u.name)).map(UserDto::of).collect(Collectors.toList()),
                p.teams.stream().map(this::team).collect(Collectors.toList()), total, done, progress);
    }

    public static boolean isComplete(TaskStatus s) { return s == TaskStatus.ACCEPTED || s == TaskStatus.DONE; }

    public TaskDto task(Task t) {
        return new TaskDto(t.id, t.title, t.description, t.status, t.startDate, t.deadline, t.project.id, t.project.name,
                UserDto.of(t.assignedTo), UserDto.of(t.createdBy), t.rating, t.reviewFeedback, t.createdAt, t.updatedAt);
    }

    public ChatMessageDto message(ChatMessage m) {
        return new ChatMessageDto(m.id, m.channel, UserDto.of(m.sender), m.receiver == null ? null : m.receiver.id,
                m.project == null ? null : m.project.id, m.text, m.sentAt);
    }

    public NotificationDto notification(Notification n) {
        return new NotificationDto(n.id, n.message, n.link, n.read, n.createdAt);
    }

    public AttachmentDto attachment(Attachment a) {
        return new AttachmentDto(a.id, a.fileName, a.kind, UserDto.of(a.uploadedBy), a.createdAt);
    }
}
