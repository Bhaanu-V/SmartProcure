package com.smartprocure.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartprocure.dto.auth.LoginRequest;
import com.smartprocure.dto.auth.RegisterRequest;
import com.smartprocure.entity.Role;
import com.smartprocure.entity.User;
import com.smartprocure.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String TEST_USERNAME = "auth_it_test_user";
    private static final String TEST_PASSWORD = "Password123!";
    private static final String REGISTERED_USERNAME = "registered_it_user";

    @BeforeEach
    void setUp() {
        userRepository.findByUsername(TEST_USERNAME).ifPresent(userRepository::delete);
        userRepository.findByUsername(REGISTERED_USERNAME).ifPresent(userRepository::delete);

        User testUser = new User(
                TEST_USERNAME,
                "auth_it@smartprocure.com",
                passwordEncoder.encode(TEST_PASSWORD),
                "Auth IT User",
                Role.ADMIN
        );
        userRepository.save(testUser);
    }

    @AfterEach
    void tearDown() {
        userRepository.findByUsername(TEST_USERNAME).ifPresent(userRepository::delete);
        userRepository.findByUsername(REGISTERED_USERNAME).ifPresent(userRepository::delete);
    }

    @Test
    @DisplayName("POST /api/v1/auth/login with valid credentials returns 200 OK and JWT access token")
    void testLoginValidCredentials() throws Exception {
        LoginRequest loginRequest = new LoginRequest(TEST_USERNAME, TEST_PASSWORD);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.username").value(TEST_USERNAME))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login with invalid password returns 401 Unauthorized")
    void testLoginInvalidPassword() throws Exception {
        LoginRequest loginRequest = new LoginRequest(TEST_USERNAME, "IncorrectPassword!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me without Bearer token returns 401 Unauthorized")
    void testGetMeUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/auth/me with valid Bearer token returns user profile")
    void testGetMeAuthorized() throws Exception {
        LoginRequest loginRequest = new LoginRequest(TEST_USERNAME, TEST_PASSWORD);

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String token = json.get("accessToken").asText();

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(TEST_USERNAME))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register registers new user and allows immediate login")
    void testRegisterAndLogin() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                REGISTERED_USERNAME,
                "registered_it@smartprocure.com",
                "Password123!",
                "Integration Test User",
                null,
                Role.EMPLOYEE
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(REGISTERED_USERNAME))
                .andExpect(jsonPath("$.role").value("EMPLOYEE"));

        LoginRequest loginRequest = new LoginRequest(REGISTERED_USERNAME, "Password123!");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.username").value(REGISTERED_USERNAME));
    }
}
