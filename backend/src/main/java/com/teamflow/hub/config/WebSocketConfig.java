package com.teamflow.hub.config;

import com.teamflow.hub.model.ChannelType;
import com.teamflow.hub.model.User;
import com.teamflow.hub.repo.ProjectRepository;
import com.teamflow.hub.repo.UserRepository;
import com.teamflow.hub.security.JwtService;
import com.teamflow.hub.service.ProjectService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * STOMP over WebSocket at /ws. The client sends its JWT in the STOMP CONNECT "Authorization" header.
 * Clients only subscribe (messages are posted through the REST API, which pushes them to subscribers).
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private static final Pattern PROJECT_TOPIC = Pattern.compile("^/topic/project\\.(\\d+)\\.(project|announcement)$");

    private final JwtService jwt;
    private final UserRepository users;
    private final ProjectRepository projects;
    private final String[] origins;

    public WebSocketConfig(JwtService jwt, UserRepository users, ProjectRepository projects,
                           @Value("${app.cors.allowed-origins}") String[] origins) {
        this.jwt = jwt;
        this.users = users;
        this.projects = projects;
        this.origins = origins;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOrigins(origins);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setUserDestinationPrefix("/user");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor acc = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (acc == null) return message;
                if (StompCommand.CONNECT.equals(acc.getCommand())) {
                    String header = acc.getFirstNativeHeader("Authorization");
                    String email = header != null && header.startsWith("Bearer ")
                            ? jwt.validate(header.substring(7)).orElse(null) : null;
                    if (email == null || users.findByEmailIgnoreCase(email).isEmpty()) {
                        throw new MessagingException("Unauthorized");
                    }
                    acc.setUser(new UsernamePasswordAuthenticationToken(email, null, java.util.List.of()));
                } else if (StompCommand.SUBSCRIBE.equals(acc.getCommand())) {
                    Principal p = acc.getUser();
                    if (p == null) throw new MessagingException("Unauthorized");
                    String dest = acc.getDestination() == null ? "" : acc.getDestination();
                    if (dest.startsWith("/topic/")) {
                        Matcher m = PROJECT_TOPIC.matcher(dest);
                        if (!m.matches()) throw new MessagingException("Forbidden destination");
                        User u = users.findByEmailIgnoreCase(p.getName()).orElseThrow(() -> new MessagingException("Unauthorized"));
                        boolean ok = projects.findVisibleTo(u).stream().anyMatch(pr -> pr.id.equals(Long.valueOf(m.group(1))));
                        if (!ok) throw new MessagingException("Forbidden");
                    } else if (!dest.startsWith("/user/queue/")) {
                        throw new MessagingException("Forbidden destination");
                    }
                }
                return message;
            }
        });
    }
}
