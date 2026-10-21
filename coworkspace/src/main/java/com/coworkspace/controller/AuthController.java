package com.coworkspace.controller;

import com.coworkspace.dto.AuthDtos.AuthResponse;
import com.coworkspace.dto.AuthDtos.LoginRequest;
import com.coworkspace.dto.AuthDtos.RegisterRequest;
import com.coworkspace.dto.OtpDtos.*;
import com.coworkspace.service.AuthService;
import com.coworkspace.service.OtpService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final OtpService otpService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/otp/send")
    public ResponseEntity<SendOtpResponse> sendOtp(
            @Valid @RequestBody SendOtpRequest request,
            HttpServletRequest servletRequest
    ) {
        String clientIp = getClientIp(servletRequest);
        return ResponseEntity.ok(otpService.sendOtp(request, clientIp));
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<VerifyOtpResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(authService.verifyOtpAndAuthenticate(request));
    }

    @PostMapping("/phone/register")
    public ResponseEntity<AuthResponse> registerWithPhone(@Valid @RequestBody PhoneRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerWithPhone(request));
    }

    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
