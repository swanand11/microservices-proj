package com.portfolens.service;

import com.portfolens.dto.AuthResponse;
import com.portfolens.dto.LoginRequest;
import com.portfolens.dto.RegisterRequest;
import com.portfolens.dto.UserResponse;
import com.portfolens.entity.User;
import com.portfolens.exception.ApiException;
import com.portfolens.repository.UserRepository;
import com.portfolens.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository users; private final PasswordEncoder passwordEncoder; private final JwtService jwtService;
    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) { this.users = users; this.passwordEncoder = passwordEncoder; this.jwtService = jwtService; }
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) throw new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED", "An account with this email already exists");
        User user = users.save(new User(request.fullName().trim(), email, passwordEncoder.encode(request.password())));
        return UserResponse.from(user);
    }
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = users.findByEmailIgnoreCase(request.email().trim()).orElseThrow(() -> invalidCredentials());
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) throw invalidCredentials();
        return new AuthResponse(jwtService.generate(user.getId(), user.getEmail()), "Bearer", jwtService.getExpirationSeconds(), UserResponse.from(user));
    }
    private ApiException invalidCredentials() { return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Email or password is incorrect"); }
}
