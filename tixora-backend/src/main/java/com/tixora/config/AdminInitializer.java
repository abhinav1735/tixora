
package com.tixora.config;

import com.tixora.entity.Role;
import com.tixora.entity.User;
import com.tixora.repository.UserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Component
public class AdminInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public AdminInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.email:}") String adminEmail,
            @Value("${app.admin.password:}") String adminPassword
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        if (adminEmail.isBlank() && adminPassword.isBlank()) {
            return;
        }

        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            throw new IllegalStateException(
                    "Both admin email and password must be configured."
            );
        }

        if (adminPassword.length() < 12
                || adminPassword.getBytes(
                java.nio.charset.StandardCharsets.UTF_8
        ).length > 72) {
            throw new IllegalStateException(
                    "Admin password must be at least 12 characters "
                            + "and no more than 72 UTF-8 bytes."
            );
        }

        String normalizedEmail =
                adminEmail.trim().toLowerCase(Locale.ROOT);

        var existingUser = userRepository.findByEmail(normalizedEmail);

        if (existingUser.isPresent()) {

            if (existingUser.get().getRole() != Role.ADMIN) {
                throw new IllegalStateException(
                        "Configured admin email already belongs "
                                + "to a non-admin account."
                );
            }

            return;
        }

        User admin = new User();
        admin.setFullName("Tixora Administrator");
        admin.setEmail(normalizedEmail);
        admin.setPasswordHash(
                passwordEncoder.encode(adminPassword)
        );
        admin.setRole(Role.ADMIN);
        admin.setEnabled(true);

        userRepository.save(admin);
    }
}
