package de.thfamily18.restaurant_backend.config;

import de.thfamily18.restaurant_backend.entity.Role;
import de.thfamily18.restaurant_backend.entity.User;
import de.thfamily18.restaurant_backend.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@Configuration
@Slf4j
public class AdminSeeder {

    @Bean
    public org.springframework.boot.CommandLineRunner seedAdmin(
            UserRepository userRepo,
            PasswordEncoder encoder,
            @Value("${app.admin.email}") String email,
            @Value("${app.admin.password}") String password
    ) {
        return args -> {
            log.info("Seeding admin user...");
            if (!userRepo.existsByEmail(email)) {

                User admin = User.builder()
                        .email(email)
                        .passwordHash(encoder.encode(password))
                        .role(Role.ADMIN)
                        .createdAt(LocalDateTime.now())
                        .build();

                userRepo.save(admin);
            }
        };
    }
}
