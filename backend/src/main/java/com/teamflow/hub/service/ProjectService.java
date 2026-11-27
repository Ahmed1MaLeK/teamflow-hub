package com.teamflow.hub.service;

import com.teamflow.hub.dto.Dtos.*;
import com.teamflow.hub.model.*;
import com.teamflow.hub.repo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProjectService {
    private final ProjectRepository projects;
    private final UserRepository users;
    private final TaskRepository tasks;
    private final CurrentUser current;
    private final Mapper mapper;
    private final NotificationService notifications;

    public ProjectService(ProjectRepository projects, UserRepository users, TaskRepository tasks, CurrentUser current,
                          Mapper mapper, NotificationService notifications) {
        this.projects = projects;
        this.users = users;
        this.tasks = tasks;
        this.current = current;
        this.mapper = mapper;
        this.notifications = notifications;
    }

    /** Project the current user may see (manager or member), otherwise 404/403. */
    public Project loadVisible(Long id, User me) {
        Project p = projects.findById(id).orElseThrow(() -> ApiException.notFound("Project"));
        if (!canSee(p, me)) throw ApiException.forbidden("You are not part of this project");
        return p;
    }

    public Project loadManaged(Long id, User me) {
        Project p = projects.findById(id).orElseThrow(() -> ApiException.notFound("Project"));
        if (!p.manager.id.equals(me.id)) throw ApiException.forbidden("Only the project manager can do this");
        return p;
    }

    public static boolean canSee(Project p, User me) {
        return p.manager.id.equals(me.id) || p.members.stream().anyMatch(m -> m.id.equals(me.id));
    }

    @Transactional(readOnly = true)
    public List<ProjectDto> list() {
        return projects.findVisibleTo(current.get()).stream().map(mapper::project).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProjectDto get(Long id) { return mapper.project(loadVisible(id, current.get())); }

    @Transactional
    public ProjectDto create(ProjectRequest r) {
        User me = current.get();
        if (me.role != Role.ADMIN) throw ApiException.forbidden("Only managers can create projects");
        validateDates(r);
        Project p = new Project();
        apply(p, r);
        p.manager = me;
        return mapper.project(projects.save(p));
    }

    @Transactional
    public ProjectDto update(Long id, ProjectRequest r) {
        Project p = loadManaged(id, current.get());
        validateDates(r);
        apply(p, r);
        return mapper.project(p);
    }

    @Transactional
    public void delete(Long id) {
        Project p = loadManaged(id, current.get());
        tasks.deleteAll(tasks.findByProjectOrderByCreatedAtDesc(p));
        projects.delete(p);
    }

    @Transactional
    public ProjectDto addMember(Long id, AddMemberRequest r) {
        User me = current.get();
        Project p = loadManaged(id, me);
        User u = users.findByUsernameIgnoreCase(r.username().replaceFirst("^@", "").trim())
                .orElseThrow(() -> ApiException.badRequest("No user with that username"));
        if (u.id.equals(p.manager.id)) throw ApiException.badRequest("The manager is already part of the project");
        if (p.members.add(u)) {
            notifications.notify(u, "You were added to project \"" + p.name + "\"", "/projects/" + p.id);
        }
        return mapper.project(p);
    }

    @Transactional
    public ProjectDto removeMember(Long id, Long userId) {
        Project p = loadManaged(id, current.get());
        p.members.removeIf(m -> m.id.equals(userId));
        // business rule: a removed member must not keep open tasks in this project
        for (Task t : tasks.findByProjectOrderByCreatedAtDesc(p)) {
            if (t.assignedTo != null && t.assignedTo.id.equals(userId) && !Mapper.isComplete(t.status)) {
                t.assignedTo = null;
                t.status = TaskStatus.PENDING;
            }
        }
        return mapper.project(p);
    }

    @Transactional
    public ProjectDto addTeam(Long id, Long teamId, TeamRepository teams) {
        Project p = loadManaged(id, current.get());
        Team t = teams.findById(teamId).orElseThrow(() -> ApiException.notFound("Team"));
        p.teams.add(t);
        for (User u : t.members) {
            if (!u.id.equals(p.manager.id) && p.members.add(u)) {
                notifications.notify(u, "You were added to project \"" + p.name + "\"", "/projects/" + p.id);
            }
        }
        return mapper.project(p);
    }

    private void validateDates(ProjectRequest r) {
        if (r.startDate() != null && r.endDate() != null && r.endDate().isBefore(r.startDate())) {
            throw ApiException.badRequest("End date cannot be before start date");
        }
    }

    private void apply(Project p, ProjectRequest r) {
        p.name = r.name().trim();
        p.description = r.description();
        p.startDate = r.startDate();
        p.endDate = r.endDate();
        if (r.status() != null) p.status = r.status();
    }
}
