package com.teamflow.hub.repo;

import com.teamflow.hub.model.ChannelType;
import com.teamflow.hub.model.ChatMessage;
import com.teamflow.hub.model.Project;
import com.teamflow.hub.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByProjectAndChannelOrderBySentAtAsc(Project project, ChannelType channel);

    @Query("select m from ChatMessage m where m.channel = com.teamflow.hub.model.ChannelType.DIRECT and " +
           "((m.sender = :a and m.receiver = :b) or (m.sender = :b and m.receiver = :a)) order by m.sentAt asc")
    List<ChatMessage> findConversation(@Param("a") User a, @Param("b") User b);
}
