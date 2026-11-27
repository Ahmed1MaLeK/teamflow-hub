package com.teamflow.hub.service;

import com.teamflow.hub.dto.Dtos.*;
import com.teamflow.hub.model.*;
import com.teamflow.hub.repo.TaskRepository;
import com.teamflow.hub.repo.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Task lifecycle:
 * PENDING -> IN_PROGRESS -> REVIEWING   (done by the assignee)
 * REVIEWING -> ACCEPTED (with 1-5 rating) or back to IN_PROGRESS with feedback  (done by the project manager)
 * ACCEPTED -> DONE (manager closes the task)
 */
@Service
public class TaskService {
    private final TaskRepository tasks;
    private final UserRepository users;
    private final ProjectService projects;
    private final CurrentUser current;
    private final Mapper mapper;
    private final NotificationService notifications;

    public TaskService(TaskRepository tasks, UserRepository users, ProjectService projects, CurrentUser current,
                       Mapper mapper, NotificationService notifications) {
        this.tasks = tasks;
        this.users = users;
        this.projects = projects;
        this.current = current;
        this.mapper = mapper;
        this.notifications = notifications;
    }

    Task loadVisible(Long id, User me) {
        Task t = tasks.findById(id).orElseThrow(() -> ApiException.notFound("Task"));
        projects.loadVisible(t.project.id, me);
        return t;
    }

    @Transactional(readOnly = true)
    public List<TaskDto> list(Long projectId) {
        User me = current.get();
        List<Task> found = projectId == null ? tasks.findVisibleTo(me)
                : tasks.findByProjectOrderByCreatedAtDesc(projects.loadVisible(projectId, me));
        return found.stream().map(mapper::task).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TaskDto get(Long id) { return mapper.task(loadVisible(id, current.get())); }

    @Transactional
    public TaskDto create(TaskRequest r) {
        User me = current.get();
        Project p = projects.loadManaged(r.projectId(), me);
        checkDates(r);
        Task t = new Task();
        t.project = p;
        t.createdBy = me;
        apply(t, r, p);
        tasks.save(t);
        if (t.assignedTo != null) {
            notifications.notify(t.assignedTo, "New task assigned: \"" + t.title + "\"", "/tasks/" + t.id);
        }
        return mapper.task(t);
    }

    @Transactional
    public TaskDto update(Long id, TaskRequest r) {
        User me = current.get();
        Task t = tasks.findById(id).orElseThrow(() -> ApiException.notFound("Task"));
        projects.loadManaged(t.project.id, me);
        checkDates(r);
        User before = t.assignedTo;
        apply(t, r, t.project);
        t.updatedAt = Instant.now();
        if (t.assignedTo != null && (before == null || !before.id.equals(t.assignedTo.id))) {
            t.status = TaskStatus.PENDING;
            t.rating = null;
            t.reviewFeedback = null;
            notifications.notify(t.assignedTo, "New task assigned: \"" + t.title + "\"", "/tasks/" + t.id);
        }
        return mapper.task(t);
    }

    @Transactional
    public void delete(Long id) {
        Task t = tasks.findById(id).orElseThrow(() -> ApiException.notFound("Task"));
        projects.loadManaged(t.project.id, current.get());
        tasks.delete(t);
    }

    /** Status change by the assignee (PENDING->IN_PROGRESS->REVIEWING) or manager (ACCEPTED->DONE). */
    @Transactional
    public TaskDto changeStatus(Long id, TaskStatusRequest r) {
        User me = current.get();
        Task t = loadVisible(id, me);
        boolean isManager = t.project.manager.id.equals(me.id);
        boolean isAssignee = t.assignedTo != null && t.assignedTo.id.equals(me.id);
        TaskStatus from = t.status, to = r.status();
        if (from == to) return mapper.task(t);

        boolean assigneeMove = (from == TaskStatus.PENDING && to == TaskStatus.IN_PROGRESS)
                || (from == TaskStatus.IN_PROGRESS && to == TaskStatus.REVIEWING);
        boolean managerMove = from == TaskStatus.ACCEPTED && to == TaskStatus.DONE;

        if (assigneeMove && isAssignee) {
            t.status = to;
            if (to == TaskStatus.REVIEWING) {
                notifications.notify(t.project.manager, me.name + " submitted \"" + t.title + "\" for review", "/tasks/" + t.id);
            }
        } else if (managerMove && isManager) {
            t.status = to;
        } else if (to == TaskStatus.ACCEPTED || (from == TaskStatus.REVIEWING && to == TaskStatus.IN_PROGRESS)) {
            throw ApiException.badRequest("Use the review action to accept or reject a task");
        } else {
            throw ApiException.forbidden("This status change is not allowed (" + from + " -> " + to + ") for you");
        }
        t.updatedAt = Instant.now();
        return mapper.task(t);
    }

    /** Manager review of a task that is in REVIEWING. */
    @Transactional
    public TaskDto review(Long id, ReviewRequest r) {
        User me = current.get();
        Task t = tasks.findById(id).orElseThrow(() -> ApiException.notFound("Task"));
        projects.loadManaged(t.project.id, me);
        if (t.status != TaskStatus.REVIEWING) throw ApiException.badRequest("Only tasks in REVIEWING can be reviewed");
        if (r.accept()) {
            if (r.rating() == null) throw ApiException.badRequest("A rating (1-5) is required to accept a task");
            t.status = TaskStatus.ACCEPTED;
            t.rating = r.rating();
            t.reviewFeedback = r.feedback();
            notifications.notify(t.assignedTo, "Your task \"" + t.title + "\" was accepted (" + r.rating() + "/5)", "/tasks/" + t.id);
        } else {
            if (r.feedback() == null || r.feedback().isBlank()) throw ApiException.badRequest("Feedback is required when requesting rework");
            t.status = TaskStatus.IN_PROGRESS;
            t.rating = null;
            t.reviewFeedback = r.feedback();
            notifications.notify(t.assignedTo, "Rework requested on \"" + t.title + "\": " + r.feedback(), "/tasks/" + t.id);
        }
        t.updatedAt = Instant.now();
        return mapper.task(t);
    }

    private void checkDates(TaskRequest r) {
        if (r.startDate() != null && r.deadline() != null && r.deadline().isBefore(r.startDate())) {
            throw ApiException.badRequest("Deadline cannot be before the start date");
        }
    }

    private void apply(Task t, TaskRequest r, Project p) {
        t.title = r.title().trim();
        t.description = r.description();
        t.startDate = r.startDate();
        t.deadline = r.deadline();
        if (r.assignedToId() == null) {
            t.assignedTo = null;
        } else {
            User u = users.findById(r.assignedToId()).orElseThrow(() -> ApiException.badRequest("Assignee not found"));
            boolean inProject = p.members.stream().anyMatch(m -> m.id.equals(u.id)) || p.manager.id.equals(u.id);
            if (!inProject) throw ApiException.badRequest("The assignee must be a member of the project");
            t.assignedTo = u;
        }
    }
}
