package com.teamflow.hub.web;

import com.teamflow.hub.dto.Dtos.*;
import com.teamflow.hub.model.Attachment;
import com.teamflow.hub.repo.TeamRepository;
import com.teamflow.hub.service.AttachmentService;
import com.teamflow.hub.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ProjectController {
    private final ProjectService projects;
    private final AttachmentService attachments;
    private final TeamRepository teams;

    public ProjectController(ProjectService projects, AttachmentService attachments, TeamRepository teams) {
        this.projects = projects;
        this.attachments = attachments;
        this.teams = teams;
    }

    @GetMapping("/projects") public List<ProjectDto> list() { return projects.list(); }
    @GetMapping("/projects/{id}") public ProjectDto get(@PathVariable Long id) { return projects.get(id); }

    @PostMapping("/projects") @ResponseStatus(HttpStatus.CREATED)
    public ProjectDto create(@Valid @RequestBody ProjectRequest r) { return projects.create(r); }

    @PutMapping("/projects/{id}")
    public ProjectDto update(@PathVariable Long id, @Valid @RequestBody ProjectRequest r) { return projects.update(id, r); }

    @DeleteMapping("/projects/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { projects.delete(id); }

    @PostMapping("/projects/{id}/members")
    public ProjectDto addMember(@PathVariable Long id, @Valid @RequestBody AddMemberRequest r) { return projects.addMember(id, r); }

    @DeleteMapping("/projects/{id}/members/{userId}")
    public ProjectDto removeMember(@PathVariable Long id, @PathVariable Long userId) { return projects.removeMember(id, userId); }

    @PostMapping("/projects/{id}/teams/{teamId}")
    public ProjectDto addTeam(@PathVariable Long id, @PathVariable Long teamId) { return projects.addTeam(id, teamId, teams); }

    @GetMapping("/projects/{id}/attachments")
    public List<AttachmentDto> files(@PathVariable Long id) { return attachments.forProject(id); }

    @PostMapping("/projects/{id}/attachments")
    public AttachmentDto upload(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return attachments.uploadToProject(id, file);
    }

    @GetMapping("/attachments/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        Attachment a = attachments.load(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + a.fileName.replace("\"", "") + "\"")
                .body(attachments.resource(a));
    }
}
