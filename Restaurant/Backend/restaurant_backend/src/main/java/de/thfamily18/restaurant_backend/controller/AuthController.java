package de.thfamily18.restaurant_backend.controller;

import de.thfamily18.restaurant_backend.dto.AuthResponse;
import de.thfamily18.restaurant_backend.dto.LoginRequest;
import de.thfamily18.restaurant_backend.dto.RegisterRequest;
import de.thfamily18.restaurant_backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;


    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest req
    ) {
        AuthResponse response = authService.register(req);
        log.info("register result={}", response.toString());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest req
    ) {
        AuthResponse response = authService.login(req);
        log.info("login result={}", response.toString());
        return ResponseEntity.ok(response);
    }
}