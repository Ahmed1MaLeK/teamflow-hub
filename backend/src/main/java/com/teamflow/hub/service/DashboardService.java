package com.teamflow.hub.service;

import com.teamflow.hub.dto.Dtos.*;
import com.teamflow.hub.model.*;
import com.teamflow.hub.repo.ProjectRepository;
import com.teamflow.hub.repo.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {
    private final ProjectRepository projects;
    private final TaskRepository tasks;
    private final CurrentUser current;
    private final Mapper mapper;

    public DashboardService(ProjectRepository projects, TaskRepository tasks, CurrentUser current, Mapper mapper) {
        this.projects = projects;
        this.tasks = tasks;
        this.current = current;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public DashboardDto dashboard() {
        User me = current.get();
        List<Project> myProjects = projects.findVisibleTo(me);
        List<Task> scope = visibleTasks(me);

        Map<String, Integer> byStatus = new LinkedHashMap<>();
        for (TaskStatus s : TaskStatus.values()) byStatus.put(s.name(), 0);
        scope.forEach(t -> byStatus.merge(t.status.name(), 1, Integer::sum));

        int completed = (int) scope.stream().filter(t -> Mapper.isComplete(t.status)).count();
        int pending = scope.size() - completed;
        int efficiency = scope.isEmpty() ? 0 : Math.round(completed * 100f / scope.size());

        // tasks completed (accepted/done) per day for the last 7 days
        Map<String, Integer> perDay = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) perDay.put(today.minusDays(i).toString(), 0);
        for (Task t : scope) {
            if (Mapper.isComplete(t.status)) {
                String day = t.updatedAt.atZone(ZoneId.systemDefault()).toLocalDate().toString();
                perDay.computeIfPresent(day, (k, v) -> v + 1);
            }
        }

        List<ProjectDto> recent = myProjects.stream().limit(3).map(mapper::project).collect(Collectors.toList());
        List<RecentActivity> activity = scope.stream().sorted(Comparator.comparing((Task t) -> t.updatedAt).reversed()).limit(5)
                .map(t -> new RecentActivity(t.id, t.title, t.project.name, t.status, t.updatedAt)).collect(Collectors.toList());
        return new DashboardDto(myProjects.size(), pending, completed, efficiency, byStatus, perDay, recent,
                performance(myProjects, scope, me), activity);
    }

    @Transactional(readOnly = true)
    public InsightsDto insights() {
        User me = current.get();
        List<Task> scope = visibleTasks(me);
        List<MemberPerformance> perf = performance(projects.findVisibleTo(me), scope, me);
        List<String> highlights = new ArrayList<>();
        List<String> recs = new ArrayList<>();
        if (scope.isEmpty()) {
            return new InsightsDto("There are no tasks yet, so no insights can be generated.", highlights,
                    List.of("Create a project and add tasks to start tracking performance."));
        }
        int total = scope.size();
        long accepted = scope.stream().filter(t -> Mapper.isComplete(t.status)).count();
        long reviewing = scope.stream().filter(t -> t.status == TaskStatus.REVIEWING).count();
        long accepted_not_done = scope.stream().filter(t -> t.status == TaskStatus.ACCEPTED).count();
        LocalDate today = LocalDate.now();
        long overdue = scope.stream().filter(t -> t.deadline != null && t.deadline.isBefore(today) && !Mapper.isComplete(t.status)).count();
        long unassigned = scope.stream().filter(t -> t.assignedTo == null && !Mapper.isComplete(t.status)).count();

        highlights.add(accepted + " of " + total + " tasks are completed (" + Math.round(accepted * 100f / total) + "%).");
        perf.stream().filter(p -> p.avgRating() != null).max(Comparator.comparing(MemberPerformance::avgRating))
                .ifPresent(p -> highlights.add("Top rated member: " + p.user().name() + " (" + String.format("%.1f", p.avgRating()) + "/5)."));
        if (reviewing > 0) recs.add(reviewing + " task(s) are waiting for review. Reviewing them keeps work moving.");
        if (accepted_not_done > 0) recs.add(accepted_not_done + " accepted task(s) are not closed yet. Mark them as Done.");
        if (overdue > 0) recs.add(overdue + " task(s) are past their deadline. Check with the assignees or move the dates.");
        if (unassigned > 0) recs.add(unassigned + " open task(s) have no assignee.");
        if (recs.isEmpty()) recs.add("Everything looks on track.");
        String summary = "Your team has " + total + " task(s) across " + projects.findVisibleTo(me).size() + " project(s). "
                + accepted + " done, " + reviewing + " in review, " + overdue + " overdue.";
        return new InsightsDto(summary, highlights, recs);
    }

    private List<Task> visibleTasks(User me) { return tasks.findVisibleTo(me); }

    private List<MemberPerformance> performance(List<Project> myProjects, List<Task> scope, User me) {
        Set<User> people = new LinkedHashSet<>();
        for (Project p : myProjects) people.addAll(p.members);
        if (me.role == Role.MEMBER) { people.clear(); people.add(me); }
        List<MemberPerformance> out = new ArrayList<>();
        for (User u : people) {
            List<Task> mine = scope.stream().filter(t -> t.assignedTo != null && t.assignedTo.id.equals(u.id)).collect(Collectors.toList());
            int done = (int) mine.stream().filter(t -> Mapper.isComplete(t.status)).count();
            OptionalDouble avg = mine.stream().filter(t -> t.rating != null).mapToInt(t -> t.rating).average();
            out.add(new MemberPerformance(UserDto.of(u), mine.size(), done, mine.isEmpty() ? 0 : Math.round(done * 100f / mine.size()),
                    avg.isPresent() ? Math.round(avg.getAsDouble() * 10) / 10.0 : null));
        }
        return out;
    }
}
