package com.teamflow.hub.repo;

import com.teamflow.hub.model.Project;
import com.teamflow.hub.model.Task;
import com.teamflow.hub.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByProjectOrderByCreatedAtDesc(Project project);
    List<Task> findByAssignedToOrderByCreatedAtDesc(User user);
    List<Task> findByProjectManagerOrderByCreatedAtDesc(User manager);
    @Query("select t from Task t where t.project.manager = :u or t.assignedTo = :u or :u member of t.project.members order by t.createdAt desc")
    List<Task> findVisibleTo(@Param("u") User u);
}
