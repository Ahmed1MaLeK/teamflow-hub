package com.teamflow.hub.config;

import com.teamflow.hub.model.Role;
import com.teamflow.hub.model.User;
import com.teamflow.hub.repo.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates a default manager account on first start so you can log in right away. */
@Component
public class DataSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String email, password;

    public DataSeeder(UserRepository users, PasswordEncoder encoder,
                      @Value("${app.seed.admin-email}") String email, @Value("${app.seed.admin-password}") String password) {
        this.users = users;
        this.encoder = encoder;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(String... args) {
        if (users.existsByEmailIgnoreCase(email)) return;
        User u = new User();
        u.name = "Admin";
        u.username = "admin";
        u.email = email;
        u.passwordHash = encoder.encode(password);
        u.role = Role.ADMIN;
        users.save(u);
        log.info("Created default manager account: {} (change the password after first login)", email);
    }
}
