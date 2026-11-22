package com.medslot.platform.identity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrapRunner implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap-admin.email:admin@medslot.io}")
    private String adminEmail;

    @Value("${app.bootstrap-admin.password:AdminPass@123}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String normalizedEmail = adminEmail.trim().toLowerCase();
        if (userRepository.findByEmail(normalizedEmail).isEmpty()) {
            User admin = new User();
            admin.setFullName("Platform Administrator");
            admin.setEmail(normalizedEmail);
            admin.setPasswordHash(passwordEncoder.encode(adminPassword));
            admin.setRole(UserRole.ADMIN);
            userRepository.save(admin);
            log.info("Bootstrapped default ADMIN account: {}", normalizedEmail);
        }
    }
}