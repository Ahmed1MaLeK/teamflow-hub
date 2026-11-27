package com.teamflow.hub.web;

import com.teamflow.hub.dto.Dtos.*;
import com.teamflow.hub.model.AttachmentKind;
import com.teamflow.hub.service.AttachmentService;
import com.teamflow.hub.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api")
public class TaskController {
    private final TaskService tasks;
    private final AttachmentService attachments;

    public TaskController(TaskService tasks, AttachmentService attachments) {
        this.tasks = tasks;
        this.attachments = attachments;
    }

    @GetMapping("/tasks") public List<TaskDto> list(@RequestParam(required = false) Long projectId) { return tasks.list(projectId); }
    @GetMapping("/tasks/{id}") public TaskDto get(@PathVariable Long id) { return tasks.get(id); }

    @PostMapping("/tasks") @ResponseStatus(HttpStatus.CREATED)
    public TaskDto create(@Valid @RequestBody TaskRequest r) { return tasks.create(r); }

    @PutMapping("/tasks/{id}")
    public TaskDto update(@PathVariable Long id, @Valid @RequestBody TaskRequest r) { return tasks.update(id, r); }

    @DeleteMapping("/tasks/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { tasks.delete(id); }

    @PatchMapping("/tasks/{id}/status")
    public TaskDto status(@PathVariable Long id, @Valid @RequestBody TaskStatusRequest r) { return tasks.changeStatus(id, r); }

    @PostMapping("/tasks/{id}/review")
    public TaskDto review(@PathVariable Long id, @Valid @RequestBody ReviewRequest r) { return tasks.review(id, r); }

    @GetMapping("/tasks/{id}/attachments")
    public List<AttachmentDto> files(@PathVariable Long id) { return attachments.forTask(id); }

    @PostMapping("/tasks/{id}/attachments")
    public AttachmentDto upload(@PathVariable Long id, @RequestParam AttachmentKind kind, @RequestParam("file") MultipartFile file) {
        return attachments.uploadToTask(id, kind, file);
    }
}
