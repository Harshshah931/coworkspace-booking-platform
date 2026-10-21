package com.coworkspace.controller;

import com.coworkspace.dto.OtpDtos.PhoneRegisterRequest;
import com.coworkspace.dto.OtpDtos.SendOtpRequest;
import com.coworkspace.dto.OtpDtos.VerifyOtpRequest;
import com.coworkspace.entity.OtpChannel;
import com.coworkspace.entity.OtpPurpose;
import com.coworkspace.entity.Role;
import com.coworkspace.entity.User;
import com.coworkspace.notification.MockNotificationGateway;
import com.coworkspace.repository.OtpChallengeRepository;
import com.coworkspace.repository.RegistrationTokenRepository;
import com.coworkspace.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@TestPropertySource(properties = "otp.mock.expose-code=true")
class PhoneAuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OtpChallengeRepository otpChallengeRepository;

    @Autowired
    private RegistrationTokenRepository registrationTokenRepository;

    @Autowired
    private MockNotificationGateway mockNotificationGateway;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        otpChallengeRepository.deleteAll();
        registrationTokenRepository.deleteAll();
        userRepository.deleteAll();
        mockNotificationGateway.clear();
    }

    @Test
    @DisplayName("Normalizes 10-digit Indian phones and rejects malformed numbers")
    void testPhoneValidation_NormalizesIndianAndRejectsInvalid() throws Exception {
        SendOtpRequest indianNumber = new SendOtpRequest("9876543210", OtpChannel.SMS, OtpPurpose.AUTH);
        mockMvc.perform(post("/api/auth/otp/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(indianNumber)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("+919876543210"))
                .andExpect(jsonPath("$.deliveryProvider").value("MOCK"));

        mockMvc.perform(get("/api/dev/otp/latest").param("phone", "+919876543210"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").isNotEmpty());

        // Alphanumeric phone
        SendOtpRequest reqAlpha = new SendOtpRequest("+1415abc1234", OtpChannel.SMS, OtpPurpose.AUTH);
        mockMvc.perform(post("/api/auth/otp/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqAlpha)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Existing user phone login: Request OTP -> Verify -> Return JWT directly")
    void testExistingUser_PhoneLogin_ReturnsJwt() throws Exception {
        String phone = "+14155552671";
        User existingUser = User.builder()
                .name("Bob Manager")
                .email("bob@example.com")
                .password(passwordEncoder.encode("SecretPass123"))
                .phone(phone)
                .phoneVerified(true)
                .role(Role.USER)
                .build();
        userRepository.save(existingUser);

        // 1. Send OTP
        SendOtpRequest sendReq = new SendOtpRequest(phone, OtpChannel.WHATSAPP, OtpPurpose.AUTH);
        mockMvc.perform(post("/api/auth/otp/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OTP_SENT"))
                .andExpect(jsonPath("$.channel").value("WHATSAPP"));

        String code = mockNotificationGateway.getLatestOtp(phone);
        assertNotNull(code);

        // 2. Verify OTP
        VerifyOtpRequest verifyReq = new VerifyOtpRequest(phone, code, OtpPurpose.AUTH);
        MvcResult result = mockMvc.perform(post("/api/auth/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newUser").value(false))
                .andExpect(jsonPath("$.authResponse.name").value("Bob Manager"))
                .andExpect(jsonPath("$.authResponse.email").value("bob@example.com"))
                .andExpect(jsonPath("$.authResponse.token").isNotEmpty())
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        String jwtToken = jsonNode.get("authResponse").get("token").asText();

        // 3. Verify access to protected endpoint with issued JWT
        mockMvc.perform(get("/api/bookings/my")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("New user phone flow: Request OTP -> Verify -> RegistrationToken -> Complete Registration -> JWT")
    void testNewUser_PhoneRegistrationFlow() throws Exception {
        String phone = "+919876543210";

        // 1. Request OTP
        SendOtpRequest sendReq = new SendOtpRequest(phone, OtpChannel.SMS, OtpPurpose.REGISTRATION);
        mockMvc.perform(post("/api/auth/otp/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendReq)))
                .andExpect(status().isOk());

        String code = mockNotificationGateway.getLatestOtp(phone);
        assertNotNull(code);

        // 2. Verify OTP: returns isNewUser=true and registrationToken
        VerifyOtpRequest verifyReq = new VerifyOtpRequest(phone, code, OtpPurpose.REGISTRATION);
        MvcResult verifyResult = mockMvc.perform(post("/api/auth/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newUser").value(true))
                .andExpect(jsonPath("$.registrationToken").isNotEmpty())
                .andReturn();

        JsonNode verifyNode = objectMapper.readTree(verifyResult.getResponse().getContentAsString());
        String regToken = verifyNode.get("registrationToken").asText();

        // 3. Complete Registration with registration token
        PhoneRegisterRequest registerReq = new PhoneRegisterRequest(
                regToken,
                "Alice Wonder",
                "alice.wonder@example.com",
                "Password123"
        );

        MvcResult regResult = mockMvc.perform(post("/api/auth/phone/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Alice Wonder"))
                .andExpect(jsonPath("$.email").value("alice.wonder@example.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        // Verify user persisted with verified phone
        User createdUser = userRepository.findByEmail("alice.wonder@example.com").orElseThrow();
        assertEquals(phone, createdUser.getPhone());
        assertTrue(createdUser.isPhoneVerified());
        assertEquals(Role.USER, createdUser.getRole());

        // Reusing the registration token must fail
        mockMvc.perform(post("/api/auth/phone/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Registration token has expired or has already been used"));
    }

    @Test
    @DisplayName("Should prevent duplicate phone registration attempts")
    void testPreventDuplicatePhoneRegistration() throws Exception {
        String phone = "+919999988888";
        User existingUser = User.builder()
                .name("Charlie Existing")
                .email("charlie@example.com")
                .password(passwordEncoder.encode("Pass123"))
                .phone(phone)
                .phoneVerified(true)
                .role(Role.USER)
                .build();
        userRepository.save(existingUser);

        // Generate a fake registration token for the same phone
        String fakeToken = "reg_test_token_dup";
        registrationTokenRepository.save(com.coworkspace.entity.RegistrationToken.builder()
                .token(fakeToken)
                .phone(phone)
                .createdAt(java.time.Instant.now())
                .expiresAt(java.time.Instant.now().plusSeconds(600))
                .consumed(false)
                .build());

        PhoneRegisterRequest dupReq = new PhoneRegisterRequest(
                fakeToken,
                "Impostor",
                "impostor@example.com",
                "Password123"
        );

        mockMvc.perform(post("/api/auth/phone/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dupReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Phone number is already registered to an account"));
    }

    @Test
    @DisplayName("Unauthenticated request to protected endpoint must return 401/403")
    void testProtectedEndpoint_RequiresJwt() throws Exception {
        mockMvc.perform(get("/api/bookings/my"))
                .andExpect(status().isForbidden());
    }
}
