package com.coworkspace.controller;

import com.coworkspace.dto.AuthDtos.LoginRequest;
import com.coworkspace.dto.AuthDtos.RegisterRequest;
import com.coworkspace.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EmailAuthRegressionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Existing email registration flow continues to function")
    void testEmailRegister_Success() throws Exception {
        RegisterRequest request = new RegisterRequest("Traditional User", "traditional@example.com", "Password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Traditional User"))
                .andExpect(jsonPath("$.email").value("traditional@example.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    @DisplayName("Existing email login flow continues to function")
    void testEmailLogin_Success() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest("Login User", "login@example.com", "Password123");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = new LoginRequest("login@example.com", "Password123");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Login User"))
                .andExpect(jsonPath("$.email").value("login@example.com"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    @DisplayName("Email addresses are normalized to prevent case-variant duplicates")
    void testEmailRegistration_NormalizesCaseAndRejectsDuplicates() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                "Case User", "CaseUser@example.com", "Password123"
        );
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("caseuser@example.com"));

        RegisterRequest duplicateRequest = new RegisterRequest(
                "Duplicate User", "CASEUSER@example.com", "Password123"
        );
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isBadRequest());

        LoginRequest caseInsensitiveLogin = new LoginRequest("CASEUSER@example.com", "Password123");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(caseInsensitiveLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("caseuser@example.com"));
    }

    @Test
    @DisplayName("Invalid email password returns 401 Unauthorized")
    void testEmailLogin_BadCredentials() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest("Login User", "login2@example.com", "Password123");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        LoginRequest badLogin = new LoginRequest("login2@example.com", "WrongPassword");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badLogin)))
                .andExpect(status().isUnauthorized());
    }
}
