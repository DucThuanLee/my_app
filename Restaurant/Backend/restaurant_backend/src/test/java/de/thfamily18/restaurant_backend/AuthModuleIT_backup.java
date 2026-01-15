package de.thfamily18.restaurant_backend;

import de.thfamily18.restaurant_backend.entity.Role;
import de.thfamily18.restaurant_backend.entity.User;
import de.thfamily18.restaurant_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
class AuthModuleIT_backup {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper om;

    @Autowired
    UserRepository userRepo;
    @Autowired
    PasswordEncoder encoder;

    @BeforeEach
    void cleanDb() {
        userRepo.deleteAll();
    }

    // ===== helper =====
    private String loginAndGetToken(String email, String password) throws Exception {
        String body = """
                  {"email":"%s","password":"%s"}
                """.formatted(email, password);

        String res = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", not(blankOrNullString())))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return om.readTree(res).get("accessToken").asText();
    }

    // ===== tests =====

    @Test
    void register_shouldCreateUser_andReturnJwt() throws Exception {
        String body = """
                  {"email":"user1@test.de","password":"Password123!"}
                """;

        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())  // nếu bạn trả 200 thì đổi thành isOk()
                .andExpect(jsonPath("$.accessToken", not(blankOrNullString())));

        // verify user saved
        var u = userRepo.findByEmail("user1@test.de").orElseThrow();
        assertEquals(Role.USER, u.getRole());
        assertTrue(encoder.matches("Password123!", u.getPasswordHash()));
    }

    @Test
    void register_duplicateEmail_should409() throws Exception {
        userRepo.save(User.builder()
                .email("dup@test.de")
                .passwordHash(encoder.encode("Password123!"))
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .build());

        String body = """
                  {"email":"dup@test.de","password":"Password123!"}
                """;

        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void login_valid_shouldReturnJwt() throws Exception {
        userRepo.save(User.builder()
                .email("user@test.de")
                .passwordHash(encoder.encode("Password123!"))
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .build());

        String body = """
                  {"email":"user@test.de","password":"Password123!"}
                """;

        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", not(blankOrNullString())));
    }

    @Test
    void login_wrongPassword_should401() throws Exception {
        userRepo.save(User.builder()
                .email("user@test.de")
                .passwordHash(encoder.encode("Password123!"))
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .build());

        String body = """
                  {"email":"user@test.de","password":"WrongPassword!"}
                """;

        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_withoutToken_should401() throws Exception {
        mvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_withToken_shouldReturnEmailAndRole() throws Exception {
        userRepo.save(User.builder()
                .email("me@test.de")
                .passwordHash(encoder.encode("Password123!"))
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .build());

        String token = loginAndGetToken("me@test.de", "Password123!");

        mvc.perform(get("/api/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("me@test.de")))
                .andExpect(jsonPath("$.role", is("USER")));
    }

    @Test
    void adminEndpoint_userRole_should403() throws Exception {
        userRepo.save(User.builder()
                .email("user@test.de")
                .passwordHash(encoder.encode("Password123!"))
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .build());

        String token = loginAndGetToken("user@test.de", "Password123!");

        mvc.perform(get("/api/admin/orders")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminEndpoint_adminRole_should200_orNotFoundDependingOnYourApi() throws Exception {
        userRepo.save(User.builder()
                .email("admin@test.de")
                .passwordHash(encoder.encode("Password123!"))
                .role(Role.ADMIN)
                .createdAt(LocalDateTime.now())
                .build());

        String token = loginAndGetToken("admin@test.de", "Password123!");

        // Nếu bạn có GET /api/admin/orders -> 200 OK
        mvc.perform(get("/api/admin/orders")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}