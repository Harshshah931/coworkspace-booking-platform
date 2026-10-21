package com.coworkspace.service;

import com.coworkspace.dto.AuthDtos.AuthResponse;
import com.coworkspace.dto.AuthDtos.LoginRequest;
import com.coworkspace.dto.AuthDtos.RegisterRequest;
import com.coworkspace.dto.OtpDtos.PhoneRegisterRequest;
import com.coworkspace.dto.OtpDtos.VerifyOtpRequest;
import com.coworkspace.dto.OtpDtos.VerifyOtpResponse;
import com.coworkspace.entity.OtpPurpose;
import com.coworkspace.entity.RegistrationToken;
import com.coworkspace.entity.Role;
import com.coworkspace.entity.User;
import com.coworkspace.exception.BadRequestException;
import com.coworkspace.repository.UserRepository;
import com.coworkspace.security.JwtUtil;
import com.coworkspace.security.UserPrincipal;
import com.coworkspace.util.PhoneNumberUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final OtpService otpService;

    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BadRequestException("Email is already registered");
        }

        User user = User.builder()
                .name(request.getName())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER) // all self-registrations are USER; admins seeded separately
                .build();

        userRepository.save(user);

        UserPrincipal principal = new UserPrincipal(user);
        String token = jwtUtil.generateToken(principal);

        return new AuthResponse(token, user.getName(), user.getEmail(), user.getRole().name());
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim();
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.getPassword())
        );

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadRequestException("User not found"));

        UserPrincipal principal = new UserPrincipal(user);
        String token = jwtUtil.generateToken(principal);

        return new AuthResponse(token, user.getName(), user.getEmail(), user.getRole().name());
    }

    @Transactional
    public VerifyOtpResponse verifyOtpAndAuthenticate(VerifyOtpRequest request) {
        String normalizedPhone = PhoneNumberUtil.normalizeAndValidate(request.getPhone());
        OtpPurpose purpose = request.getPurpose() != null ? request.getPurpose() : OtpPurpose.AUTH;

        // Verify challenge code under lock
        otpService.verifyOtp(normalizedPhone, request.getCode(), purpose);

        Optional<User> existingUserOpt = userRepository.findByPhone(normalizedPhone);
        if (purpose == OtpPurpose.REGISTRATION && existingUserOpt.isPresent()) {
            throw new BadRequestException("Phone number is already registered. Choose Login with OTP instead.");
        }

        if (purpose == OtpPurpose.AUTH && existingUserOpt.isEmpty()) {
            throw new BadRequestException("No account exists for this phone number. Choose Sign Up to register.");
        }

        if (existingUserOpt.isPresent()) {
            User user = existingUserOpt.get();
            UserPrincipal principal = new UserPrincipal(user);
            String token = jwtUtil.generateToken(principal);

            AuthResponse authResponse = new AuthResponse(token, user.getName(), user.getEmail(), user.getRole().name());
            return VerifyOtpResponse.builder()
                    .isNewUser(false)
                    .phone(normalizedPhone)
                    .authResponse(authResponse)
                    .build();
        } else {
            // New user: phone is verified, issue short-lived single-use registration token
            String registrationToken = otpService.createRegistrationToken(normalizedPhone);
            return VerifyOtpResponse.builder()
                    .isNewUser(true)
                    .phone(normalizedPhone)
                    .registrationToken(registrationToken)
                    .build();
        }
    }

    @Transactional
    public AuthResponse registerWithPhone(PhoneRegisterRequest request) {
        RegistrationToken regToken = otpService.validateAndConsumeRegistrationToken(request.getRegistrationToken());
        String phone = regToken.getPhone();

        if (userRepository.existsByPhone(phone)) {
            throw new BadRequestException("Phone number is already registered to an account");
        }

        String email = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BadRequestException("Email is already registered");
        }

        // Create new account strictly with Role.USER (prevent client role escalation)
        User user = User.builder()
                .name(request.getName().trim())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(phone)
                .phoneVerified(true)
                .role(Role.USER)
                .build();

        user = userRepository.save(user);

        UserPrincipal principal = new UserPrincipal(user);
        String token = jwtUtil.generateToken(principal);

        return new AuthResponse(token, user.getName(), user.getEmail(), user.getRole().name());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
