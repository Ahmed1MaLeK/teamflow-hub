package com.teamflow.hub.service;

import com.teamflow.hub.dto.Dtos.*;
import com.teamflow.hub.model.PasswordResetToken;
import com.teamflow.hub.model.Role;
import com.teamflow.hub.model.User;
import com.teamflow.hub.repo.PasswordResetTokenRepository;
import com.teamflow.hub.repo.UserRepository;
import com.teamflow.hub.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository users;
    private final PasswordResetTokenRepository resetTokens;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authManager;
    private final JwtService jwt;
    private final CurrentUser current;

    public AuthService(UserRepository users, PasswordResetTokenRepository resetTokens, PasswordEncoder encoder,
                       AuthenticationManager authManager, JwtService jwt, CurrentUser current) {
        this.users = users;
        this.resetTokens = resetTokens;
        this.encoder = encoder;
        this.authManager = authManager;
        this.jwt = jwt;
        this.current = current;
    }

    @Transactional
    public AuthResponse register(RegisterRequest r) {
        if (users.existsByEmailIgnoreCase(r.email())) throw ApiException.badRequest("Email is already registered");
        if (users.existsByUsernameIgnoreCase(r.username())) throw ApiException.badRequest("Username is already taken");
        User u = new User();
        u.name = r.name().trim();
        u.username = r.username().trim();
        u.email = r.email().trim().toLowerCase();
        u.passwordHash = encoder.encode(r.password());
        u.role = r.role() == null ? Role.MEMBER : r.role();
        users.save(u);
        return new AuthResponse(jwt.generate(u.email), UserDto.of(u));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest r) {
        authManager.authenticate(new UsernamePasswordAuthenticationToken(r.email().trim().toLowerCase(), r.password()));
        User u = users.findByEmailIgnoreCase(r.email().trim()).orElseThrow();
        return new AuthResponse(jwt.generate(u.email), UserDto.of(u));
    }

    /**
     * No mail server is configured, so the reset token is logged to the backend console and also returned
     * in the response (development convenience). Replace with an email sender before going to production.
     */
    @Transactional
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest r) {
        String msg = "If that email exists, a reset token has been generated.";
        return users.findByEmailIgnoreCase(r.email().trim()).map(u -> {
            PasswordResetToken t = new PasswordResetToken();
            t.user = u;
            t.token = UUID.randomUUID().toString();
            t.expiresAt = Instant.now().plusSeconds(30 * 60);
            resetTokens.save(t);
            log.info("Password reset token for {}: {}", u.email, t.token);
            return new ForgotPasswordResponse(msg, t.token);
        }).orElse(new ForgotPasswordResponse(msg, null));
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest r) {
        PasswordResetToken t = resetTokens.findByToken(r.token()).orElseThrow(() -> ApiException.badRequest("Invalid or expired token"));
        if (t.used || t.expiresAt.isBefore(Instant.now())) throw ApiException.badRequest("Invalid or expired token");
        t.user.passwordHash = encoder.encode(r.newPassword());
        t.used = true;
    }

    @Transactional(readOnly = true)
    public UserDto me() { return UserDto.of(current.get()); }

    @Transactional
    public UserDto updateProfile(UpdateProfileRequest r) {
        User u = current.get();
        u.name = r.name().trim();
        if (r.newPassword() != null && !r.newPassword().isBlank()) {
            if (r.newPassword().length() < 6) throw ApiException.badRequest("Password must be at least 6 characters");
            u.passwordHash = encoder.encode(r.newPassword());
        }
        return UserDto.of(u);
    }
}
