package com.teamflow.hub.repo;

import com.teamflow.hub.model.Project;
import com.teamflow.hub.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    @Query("select distinct p from Project p left join p.members m where p.manager = :u or m = :u order by p.createdAt desc")
    List<Project> findVisibleTo(@Param("u") User u);
}
