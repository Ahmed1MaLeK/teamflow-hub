package com.teamflow.hub.service;

import com.teamflow.hub.dto.Dtos.AttachmentDto;
import com.teamflow.hub.model.*;
import com.teamflow.hub.repo.AttachmentRepository;
import com.teamflow.hub.repo.TaskRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AttachmentService {
    private final AttachmentRepository repo;
    private final TaskRepository tasks;
    private final ProjectService projects;
    private final CurrentUser current;
    private final Mapper mapper;
    private final Path root;

    public AttachmentService(AttachmentRepository repo, TaskRepository tasks, ProjectService projects, CurrentUser current,
                             Mapper mapper, @Value("${app.upload-dir}") String uploadDir) throws IOException {
        this.repo = repo;
        this.tasks = tasks;
        this.projects = projects;
        this.current = current;
        this.mapper = mapper;
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    @Transactional(readOnly = true)
    public List<AttachmentDto> forProject(Long projectId) {
        Project p = projects.loadVisible(projectId, current.get());
        return repo.findByProjectOrderByCreatedAtDesc(p).stream().map(mapper::attachment).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AttachmentDto> forTask(Long taskId) {
        Task t = tasks.findById(taskId).orElseThrow(() -> ApiException.notFound("Task"));
        projects.loadVisible(t.project.id, current.get());
        return repo.findByTaskOrderByCreatedAtDesc(t).stream().map(mapper::attachment).collect(Collectors.toList());
    }

    @Transactional
    public AttachmentDto uploadToProject(Long projectId, MultipartFile file) {
        User me = current.get();
        Project p = projects.loadManaged(projectId, me);
        return save(file, me, p, null, AttachmentKind.PROJECT_FILE);
    }

    /** Manager uploads GUIDELINE files; the assignee uploads DELIVERABLE files. */
    @Transactional
    public AttachmentDto uploadToTask(Long taskId, AttachmentKind kind, MultipartFile file) {
        User me = current.get();
        Task t = tasks.findById(taskId).orElseThrow(() -> ApiException.notFound("Task"));
        boolean manager = t.project.manager.id.equals(me.id);
        boolean assignee = t.assignedTo != null && t.assignedTo.id.equals(me.id);
        if (kind == AttachmentKind.GUIDELINE && !manager) throw ApiException.forbidden("Only the manager can upload guidelines");
        if (kind == AttachmentKind.DELIVERABLE && !assignee) throw ApiException.forbidden("Only the assignee can upload deliverables");
        if (kind == AttachmentKind.PROJECT_FILE) throw ApiException.badRequest("Invalid attachment kind for a task");
        return save(file, me, t.project, t, kind);
    }

    @Transactional(readOnly = true)
    public Attachment load(Long id) {
        Attachment a = repo.findById(id).orElseThrow(() -> ApiException.notFound("Attachment"));
        projects.loadVisible(a.project.id, current.get());
        return a;
    }

    public Resource resource(Attachment a) {
        Path p = root.resolve(a.storedName).normalize();
        if (!p.startsWith(root) || !Files.exists(p)) throw ApiException.notFound("File");
        return new FileSystemResource(p);
    }

    private AttachmentDto save(MultipartFile file, User me, Project p, Task t, AttachmentKind kind) {
        if (file == null || file.isEmpty()) throw ApiException.badRequest("No file provided");
        String original = Paths.get(file.getOriginalFilename() == null ? "file" : file.getOriginalFilename()).getFileName().toString();
        String stored = UUID.randomUUID() + "-" + original.replaceAll("[^A-Za-z0-9._-]", "_");
        try {
            Files.copy(file.getInputStream(), root.resolve(stored));
        } catch (IOException e) {
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Could not store file");
        }
        Attachment a = new Attachment();
        a.project = p;
        a.task = t;
        a.kind = kind;
        a.fileName = original;
        a.storedName = stored;
        a.uploadedBy = me;
        return mapper.attachment(repo.save(a));
    }
}
