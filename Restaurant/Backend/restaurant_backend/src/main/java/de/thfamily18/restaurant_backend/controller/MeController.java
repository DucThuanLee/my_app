package de.thfamily18.restaurant_backend.controller;

import de.thfamily18.restaurant_backend.dto.MeResponse;
import de.thfamily18.restaurant_backend.entity.Role;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class MeController {
    @GetMapping
    public MeResponse me(Authentication auth) {
        // The role is in the authorities, but the standard role can be fetched from the database later.
        // Temporarily return USER if not parsed, for simplicity.
        return new MeResponse(auth.getName(), Role.USER);
    }
}
