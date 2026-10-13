package com.storeit.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storeit.dto.LoginRequest;
import com.storeit.dto.RegisterRequest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("storeit_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Shared across ordered tests
    private static String accessToken;
    private static Cookie refreshCookie;

    // ---- Registration ----

    @Test
    @Order(1)
    @DisplayName("POST /register — creates user and returns tokens")
    void register_success() throws Exception {
        RegisterRequest req = RegisterRequest.builder()
                .email("test@example.com")
                .fullName("Test User")
                .password("password123")
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("test@example.com"))
                .andExpect(jsonPath("$.user.fullName").value("Test User"))
                .andReturn();

        accessToken = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
        refreshCookie = result.getResponse().getCookie("storeit-refresh");
    }

    @Test
    @Order(2)
    @DisplayName("POST /register — duplicate email returns 409")
    void register_duplicateEmail() throws Exception {
        RegisterRequest req = RegisterRequest.builder()
                .email("test@example.com")
                .fullName("Another User")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(3)
    @DisplayName("POST /register — invalid email returns 400")
    void register_invalidEmail() throws Exception {
        RegisterRequest req = RegisterRequest.builder()
                .email("not-an-email")
                .fullName("Test")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(4)
    @DisplayName("POST /register — short password returns 400")
    void register_shortPassword() throws Exception {
        RegisterRequest req = RegisterRequest.builder()
                .email("short@example.com")
                .fullName("Test")
                .password("123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ---- Login ----

    @Test
    @Order(5)
    @DisplayName("POST /login — valid credentials returns tokens")
    void login_success() throws Exception {
        LoginRequest req = LoginRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("test@example.com"))
                .andReturn();

        // Update shared state for later tests
        accessToken = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
        refreshCookie = result.getResponse().getCookie("storeit-refresh");
    }

    @Test
    @Order(6)
    @DisplayName("POST /login — wrong password returns 404")
    void login_wrongPassword() throws Exception {
        LoginRequest req = LoginRequest.builder()
                .email("test@example.com")
                .password("wrongpassword")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(7)
    @DisplayName("POST /login — unknown email returns 404")
    void login_unknownEmail() throws Exception {
        LoginRequest req = LoginRequest.builder()
                .email("ghost@example.com")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    // ---- Profile ----

    @Test
    @Order(8)
    @DisplayName("GET /me — with valid JWT returns user profile")
    void me_authenticated() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.fullName").value("Test User"));
    }

    @Test
    @Order(9)
    @DisplayName("GET /me — without token returns 401")
    void me_unauthenticated() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    // ---- Refresh ----

    @Test
    @Order(10)
    @DisplayName("POST /refresh — with valid cookie returns new access token")
    void refresh_success() throws Exception {
        Assertions.assertNotNull(refreshCookie, "refreshCookie must be set by login");

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    @Order(11)
    @DisplayName("POST /refresh — without cookie returns 401")
    void refresh_noCookie() throws Exception {
        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized());
    }

    // ---- Logout ----

    @Test
    @Order(12)
    @DisplayName("POST /logout — invalidates refresh token")
    void logout_success() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // After logout, refresh with the old cookie should fail
        if (refreshCookie != null) {
            mockMvc.perform(post("/api/auth/refresh")
                            .cookie(refreshCookie))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    @Order(13)
    @DisplayName("POST /logout — without token returns 401")
    void logout_unauthenticated() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isUnauthorized());
    }
}
