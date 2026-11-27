package com.teamflow.hub.repo;

import com.teamflow.hub.model.Attachment;
import com.teamflow.hub.model.Project;
import com.teamflow.hub.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    List<Attachment> findByProjectOrderByCreatedAtDesc(Project project);
    List<Attachment> findByTaskOrderByCreatedAtDesc(Task task);
}
