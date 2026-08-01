package com.coworkspace.service;

import com.coworkspace.dto.AuthDtos.AuthResponse;
import com.coworkspace.dto.AuthDtos.LoginRequest;
import com.coworkspace.dto.AuthDtos.RegisterRequest;
import com.coworkspace.entity.Role;
import com.coworkspace.entity.User;
import com.coworkspace.exception.BadRequestException;
import com.coworkspace.repository.UserRepository;
import com.coworkspace.security.JwtUtil;
import com.coworkspace.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER) // all self-registrations are USER; admins seeded separately
                .build();

        userRepository.save(user);

        UserPrincipal principal = new UserPrincipal(user);
        String token = jwtUtil.generateToken(principal);

        return new AuthResponse(token, user.getName(), user.getEmail(), user.getRole().name());
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("User not found"));

        UserPrincipal principal = new UserPrincipal(user);
        String token = jwtUtil.generateToken(principal);

        return new AuthResponse(token, user.getName(), user.getEmail(), user.getRole().name());
    }
}
