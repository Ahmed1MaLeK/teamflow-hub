package com.teamflow.hub.web;

import com.teamflow.hub.dto.Dtos.*;
import com.teamflow.hub.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) { this.auth = auth; }

    @PostMapping("/auth/register") @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest r) { return auth.register(r); }

    @PostMapping("/auth/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest r) { return auth.login(r); }

    @PostMapping("/auth/forgot-password")
    public ForgotPasswordResponse forgot(@Valid @RequestBody ForgotPasswordRequest r) { return auth.forgotPassword(r); }

    @PostMapping("/auth/reset-password")
    public Map<String, String> reset(@Valid @RequestBody ResetPasswordRequest r) {
        auth.resetPassword(r);
        return Map.of("message", "Password updated");
    }

    @GetMapping("/me")
    public UserDto me() { return auth.me(); }

    @PutMapping("/me")
    public UserDto update(@Valid @RequestBody UpdateProfileRequest r) { return auth.updateProfile(r); }
}
