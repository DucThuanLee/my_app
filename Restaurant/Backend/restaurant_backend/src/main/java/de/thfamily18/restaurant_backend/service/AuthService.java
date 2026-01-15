package de.thfamily18.restaurant_backend.service;

import de.thfamily18.restaurant_backend.dto.AuthResponse;
import de.thfamily18.restaurant_backend.dto.LoginRequest;
import de.thfamily18.restaurant_backend.dto.RegisterRequest;
import de.thfamily18.restaurant_backend.entity.Role;
import de.thfamily18.restaurant_backend.entity.User;
import de.thfamily18.restaurant_backend.exception.DuplicateEmailException;
import de.thfamily18.restaurant_backend.repository.UserRepository;
import de.thfamily18.restaurant_backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository repo;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authManager;
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest req) {
        if (repo.existsByEmail(req.email())) {
            throw new DuplicateEmailException(req.email());
        }
        User u = User.builder()
                .email(req.email())
                .passwordHash(encoder.encode(req.password()))
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .build();
        repo.save(u);
        return new AuthResponse(jwtService.generate(u.getEmail(), u.getRole()));
    }

    public AuthResponse login(LoginRequest req) {
        authManager.authenticate(new UsernamePasswordAuthenticationToken(req.email(), req.password()));
        // If authentication is OK, retrieve the role from the database.
        User u = repo.findByEmail(req.email()).orElseThrow();
        return new AuthResponse(jwtService.generate(u.getEmail(), u.getRole()));
    }
}
