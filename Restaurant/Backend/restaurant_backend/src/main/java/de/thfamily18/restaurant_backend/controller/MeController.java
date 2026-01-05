package de.thfamily18.restaurant_backend.controller;

import de.thfamily18.restaurant_backend.dto.MeResponse;
import de.thfamily18.restaurant_backend.entity.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class MeController {
    @GetMapping
    public MeResponse me(Authentication auth) {
        String email = auth.getName();

        Role role = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> Role.valueOf(a.replace("ROLE_", "")))
                .findFirst()
                .orElse(Role.USER);

        return new MeResponse(email, role);
    }

// method 2
//    private final UserRepository userRepository;
//
//    public MeController(UserRepository userRepository) {
//        this.userRepository = userRepository;
//    }
//
//    @GetMapping
//    public MeResponse me(Authentication auth) {
//        User user = userRepository.findByEmail(auth.getName())
//                .orElseThrow(() -> new RuntimeException("User not found"));
//
//        return new MeResponse(user.getEmail(), user.getRole());
//    }
}
