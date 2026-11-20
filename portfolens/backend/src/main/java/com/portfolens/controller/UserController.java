package com.portfolens.controller;

import com.portfolens.dto.UpdateProfileRequest;
import com.portfolens.dto.UserResponse;
import com.portfolens.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
public class UserController {
    private final UserService userService;
    public UserController(UserService userService) { this.userService = userService; }
    @GetMapping public UserResponse getProfile(Authentication authentication) { return userService.getProfile(authentication.getName()); }
    @PutMapping public UserResponse updateProfile(Authentication authentication, @Valid @RequestBody UpdateProfileRequest request) { return userService.updateProfile(authentication.getName(), request); }
}
