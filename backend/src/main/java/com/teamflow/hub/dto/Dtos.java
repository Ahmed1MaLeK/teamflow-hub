package com.teamflow.hub.dto;

import com.teamflow.hub.model.*;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class Dtos {
    private Dtos() {}

    // ---------- auth ----------
    public record RegisterRequest(@NotBlank String name, @NotBlank @Size(min = 3, max = 50) String username,
                                  @NotBlank @Email String email, @NotBlank @Size(min = 6, max = 100) String password,
                                  Role role) {}
    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}
    public record AuthResponse(String token, UserDto user) {}
    public record ForgotPasswordRequest(@NotBlank @Email String email) {}
    public record ResetPasswordRequest(@NotBlank String token, @NotBlank @Size(min = 6, max = 100) String newPassword) {}
    public record ForgotPasswordResponse(String message, String resetToken) {}
    public record UpdateProfileRequest(@NotBlank String name, String newPassword) {}

    // ---------- users ----------
    public record UserDto(Long id, String name, String username, String email, Role role) {
        public static UserDto of(User u) {
            return u == null ? null : new UserDto(u.id, u.name, u.username, u.email, u.role);
        }
    }

    // ---------- teams ----------
    public record TeamRequest(@NotBlank String name, List<Long> memberIds) {}
    public record TeamDto(Long id, String name, List<UserDto> members) {}

    // ---------- projects ----------
    public record ProjectRequest(@NotBlank String name, String description, LocalDate startDate, LocalDate endDate,
                                 ProjectStatus status) {}
    public record AddMemberRequest(@NotBlank String username) {}
    public record ProjectDto(Long id, String name, String description, ProjectStatus status, LocalDate startDate,
                             LocalDate endDate, UserDto manager, List<UserDto> members, List<TeamDto> teams,
                             int totalTasks, int completedTasks, int progress) {}

    // ---------- tasks ----------
    public record TaskRequest(@NotBlank String title, String description, LocalDate startDate, LocalDate deadline,
                              @NotNull Long projectId, Long assignedToId) {}
    public record TaskStatusRequest(@NotNull TaskStatus status) {}
    public record ReviewRequest(@NotNull Boolean accept, @Min(1) @Max(5) Integer rating, String feedback) {}
    public record TaskDto(Long id, String title, String description, TaskStatus status, LocalDate startDate,
                          LocalDate deadline, Long projectId, String projectName, UserDto assignedTo, UserDto createdBy,
                          Integer rating, String reviewFeedback, Instant createdAt, Instant updatedAt) {}

    // ---------- attachments ----------
    public record AttachmentDto(Long id, String fileName, AttachmentKind kind, UserDto uploadedBy, Instant createdAt) {}

    // ---------- chat ----------
    public record SendMessageRequest(@NotNull ChannelType channel, Long projectId, Long receiverId,
                                     @NotBlank @Size(max = 4000) String text) {}
    public record ChatMessageDto(Long id, ChannelType channel, UserDto sender, Long receiverId, Long projectId,
                                 String text, Instant sentAt) {}
    public record ChannelsDto(List<UserDto> directs, List<ProjectRef> projects) {}
    public record ProjectRef(Long id, String name) {}

    // ---------- notifications ----------
    public record NotificationDto(Long id, String message, String link, boolean read, Instant createdAt) {}

    // ---------- dashboard ----------
    public record MemberPerformance(UserDto user, int assigned, int completed, int completionPercent, Double avgRating) {}
    public record RecentActivity(Long taskId, String title, String projectName, TaskStatus status, Instant at) {}
    public record DashboardDto(int projects, int pendingTasks, int completedTasks, int efficiencyPercent,
                               java.util.Map<String, Integer> tasksByStatus, java.util.Map<String, Integer> completedPerDay,
                               List<ProjectDto> recentProjects, List<MemberPerformance> performance,
                               List<RecentActivity> recentActivity) {}
    public record InsightsDto(String summary, List<String> highlights, List<String> recommendations) {}
}
