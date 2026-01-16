package de.thfamily18.restaurant_backend;

import de.thfamily18.restaurant_backend.entity.*;
import de.thfamily18.restaurant_backend.repository.OrderRepository;
import de.thfamily18.restaurant_backend.repository.ProductRepository;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrderModuleIT extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper om;

    @Autowired
    OrderRepository orderRepo;
    @Autowired
    ProductRepository productRepo;
    @Autowired
    UserRepository userRepo;
    @Autowired
    PasswordEncoder encoder;

    private Product p1;
    private Product p2;

    @BeforeEach
    void setup() {
        // IMPORTANT: delete in correct order because of FK
        orderRepo.deleteAll();
        productRepo.deleteAll();
        userRepo.deleteAll();

        // create products (force createdAt not null to avoid constraint issues)
        p1 = productRepo.save(Product.builder()
                .nameDe("Classic Milchtee")
                .nameEn("Classic Milk Tea")
                .descriptionDe("Schwarztee mit Milch")
                .descriptionEn("Black tea with milk")
                .price(new BigDecimal("5.50"))
                .category("MILK_TEA")
                .isBestSeller(true)
                .createdAt(LocalDateTime.now())
                .build());

        p2 = productRepo.save(Product.builder()
                .nameDe("Espresso")
                .nameEn("Espresso")
                .descriptionDe("Starker Kaffee")
                .descriptionEn("Strong coffee")
                .price(new BigDecimal("2.90"))
                .category("COFFEE")
                .isBestSeller(false)
                .createdAt(LocalDateTime.now())
                .build());
    }

    // ===== helper =====
    private void createUser(String email, String password, Role role) {
        userRepo.save(User.builder()
                .email(email)
                .passwordHash(encoder.encode(password))
                .role(role)
                .createdAt(LocalDateTime.now())
                .build());
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        String body = """
          {"email":"%s","password":"%s"}
        """.formatted(email, password);

        String res = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(not(blankOrNullString())))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return om.readTree(res).get("accessToken").asString();
    }

    // ===== tests =====


    @Test
    void guest_createOrder_should201_and_then_adminGetAll_shouldContainOrder() throws Exception {
        // 1) Create guest order
        String body = """
          {
            "customerName": "Guest A",
            "phone": "0123456789",
            "address": "Berlin",
            "paymentMethod": "COD",
            "items": [
              {"productId": "%s", "quantity": 2},
              {"productId": "%s", "quantity": 1}
            ]
          }
        """.formatted(p1.getId(), p2.getId());

        mvc.perform(post("/api/orders")
                        .header("Accept-Language", "de")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(not(blankOrNullString())))
                .andExpect(jsonPath("$.paymentMethod").value("COD"))
                .andExpect(jsonPath("$.paymentStatus").value("PENDING"))
                .andExpect(jsonPath("$.orderStatus").value("NEW"))
                // totalPrice: Avoid direct double comparisons.
                .andExpect(jsonPath("$.totalPrice").value(closeTo(13.90, 0.0001)))
                .andExpect(jsonPath("$.items", hasSize(2)));

        // 2) Login admin
        createUser("admin@test.de", "Password123!", Role.ADMIN);
        String token = loginAndGetToken("admin@test.de", "Password123!");

        // 3) Verify via admin "get all orders" API (NOT using repository)
        mvc.perform(get("/api/admin/orders")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "0")
                        .param("size", "20")
                        .header("Accept-Language", "de"))
                .andExpect(status().isOk())
                // Page<> -> content[]
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(not(blankOrNullString())))
                .andExpect(jsonPath("$.content[0].paymentMethod").value("COD"))
                .andExpect(jsonPath("$.content[0].paymentStatus").value("PENDING"))
                .andExpect(jsonPath("$.content[0].orderStatus").value("NEW"))
                .andExpect(jsonPath("$.content[0].totalPrice").value(closeTo(13.90, 0.0001)))
                .andExpect(jsonPath("$.content[0].items", hasSize(2)));
    }

    @Test
    void user_createOrder_should201_and_then_userOrders_shouldContainIt() throws Exception {
        createUser("user@test.de", "Password123!", Role.USER);
        String token = loginAndGetToken("user@test.de", "Password123!");

        // 1) Create order as logged-in user
        String body = """
      {
        "customerName": "User A",
        "phone": "099999999",
        "address": "Hamburg",
        "paymentMethod": "COD",
        "items": [
          {"productId": "%s", "quantity": 1}
        ]
      }
    """.formatted(p1.getId());

        mvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + token)
                        .header("Accept-Language", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentStatus").value("PENDING"))
                .andExpect(jsonPath("$.orderStatus").value("NEW"))
                .andExpect(jsonPath("$.totalPrice").value(closeTo(5.50, 0.0001)))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].productName").value("Classic Milk Tea"));

        // 2) Verify via API: /api/user/orders (NOT using repository)
        mvc.perform(get("/api/user/orders")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "0")
                        .param("size", "20")
                        .header("Accept-Language", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].paymentStatus").value("PENDING"))
                .andExpect(jsonPath("$.content[0].orderStatus").value("NEW"))
                .andExpect(jsonPath("$.content[0].totalPrice").value(closeTo(5.50, 0.0001)))
                .andExpect(jsonPath("$.content[0].items", hasSize(1)))
                .andExpect(jsonPath("$.content[0].items[0].productName").value("Classic Milk Tea"));
    }


    @Test
    void userOrders_requiresAuth_should401() throws Exception {
        mvc.perform(get("/api/user/orders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userOrders_withAuth_shouldReturnPage() throws Exception {
        createUser("user2@test.de", "Password123!", Role.USER);
        String token = loginAndGetToken("user2@test.de", "Password123!");

        // create an order
        String body = """
          {
            "customerName": "User2",
            "phone": "011111111",
            "address": "Munich",
            "paymentMethod": "COD",
            "items": [
              {"productId": "%s", "quantity": 1}
            ]
          }
        """.formatted(p2.getId());

        mvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + token)
                        .header("Accept-Language", "de")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/user/orders")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "0")
                        .param("size", "20")
                        .header("Accept-Language", "de"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.content[0].paymentStatus").value("PENDING"));
    }

    @Test
    void adminList_userRole_should403() throws Exception {
        createUser("user3@test.de", "Password123!", Role.USER);
        String token = loginAndGetToken("user3@test.de", "Password123!");

        mvc.perform(get("/api/admin/orders")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminList_adminRole_should200() throws Exception {
        createUser("admin@test.de", "Password123!", Role.ADMIN);
        String token = loginAndGetToken("admin@test.de", "Password123!");

        mvc.perform(get("/api/admin/orders")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk());
    }
}