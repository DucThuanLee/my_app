package de.thfamily18.restaurant_backend.controller;

import de.thfamily18.restaurant_backend.dto.MeResponse;
import de.thfamily18.restaurant_backend.entity.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Me", description = "Current authenticated user")
@RestController
@RequestMapping("/api/me")
public class MeController {

    @Operation(
            summary = "Get current user info",
            description = "Return email and role of the currently authenticated user"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authenticated user info returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized (missing or invalid JWT)")
    })
    @SecurityRequirement(name = "bearerAuth")
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
