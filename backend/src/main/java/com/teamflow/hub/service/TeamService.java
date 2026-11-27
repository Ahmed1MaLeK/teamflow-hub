package com.teamflow.hub.service;

import com.teamflow.hub.dto.Dtos.*;
import com.teamflow.hub.model.Role;
import com.teamflow.hub.model.Team;
import com.teamflow.hub.model.User;
import com.teamflow.hub.repo.TeamRepository;
import com.teamflow.hub.repo.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TeamService {
    private final TeamRepository teams;
    private final UserRepository users;
    private final CurrentUser current;
    private final Mapper mapper;

    public TeamService(TeamRepository teams, UserRepository users, CurrentUser current, Mapper mapper) {
        this.teams = teams;
        this.users = users;
        this.current = current;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<TeamDto> list() { return teams.findAll().stream().map(mapper::team).collect(Collectors.toList()); }

    @Transactional
    public TeamDto create(TeamRequest r) {
        requireAdmin();
        if (teams.existsByNameIgnoreCase(r.name().trim())) throw ApiException.badRequest("A team with this name already exists");
        Team t = new Team();
        t.name = r.name().trim();
        t.members = new HashSet<>(users.findAllById(r.memberIds() == null ? List.of() : r.memberIds()));
        return mapper.team(teams.save(t));
    }

    @Transactional
    public TeamDto update(Long id, TeamRequest r) {
        requireAdmin();
        Team t = teams.findById(id).orElseThrow(() -> ApiException.notFound("Team"));
        t.name = r.name().trim();
        t.members = new HashSet<>(users.findAllById(r.memberIds() == null ? List.of() : r.memberIds()));
        return mapper.team(t);
    }

    @Transactional
    public void delete(Long id) {
        requireAdmin();
        teams.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<UserDto> searchUsers(String q) {
        List<User> found = (q == null || q.isBlank()) ? users.findAll()
                : users.findByNameContainingIgnoreCaseOrUsernameContainingIgnoreCase(q, q);
        return found.stream().map(UserDto::of).collect(Collectors.toList());
    }

    private void requireAdmin() {
        if (current.get().role != Role.ADMIN) throw ApiException.forbidden("Only managers can manage teams");
    }
}
