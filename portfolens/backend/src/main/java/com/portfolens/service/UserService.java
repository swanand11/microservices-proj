package com.portfolens.service;

import com.portfolens.dto.UpdateProfileRequest;
import com.portfolens.dto.UserResponse;
import com.portfolens.entity.User;
import com.portfolens.exception.ApiException;
import com.portfolens.repository.UserRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository users;
    public UserService(UserRepository users) { this.users = users; }
    @Transactional(readOnly = true)
    public UserResponse getProfile(String userId) { return UserResponse.from(find(UUID.fromString(userId))); }
    @Transactional
    public UserResponse updateProfile(String userId, UpdateProfileRequest request) { User user = find(UUID.fromString(userId)); user.updateProfile(request.fullName().trim()); return UserResponse.from(user); }
    private User find(UUID id) { return users.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User profile was not found")); }
}
