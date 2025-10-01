package com.healthapp.auth.controller;

import com.healthapp.auth.dto.response.UserResponse;
import com.healthapp.auth.entity.User;
import com.healthapp.auth.repository.UserRepository;
import com.healthapp.auth.security.CustomUserPrincipal;
import com.healthapp.auth.service.UserService;

import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
@RestController
@RequestMapping("/api/v1/user")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('USER', 'DOCTOR', 'ADMIN')")

public class UserController {
    
    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getCurrentUserProfile(Authentication authentication) {
        CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
        
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        UserResponse userResponse = userService.mapToUserResponse(user);
        return ResponseEntity.ok(userResponse);
    }
    @PutMapping("/profile")
    public ResponseEntity<UserResponse> updateUserProfile(
            @RequestBody Map<String, Object> updateData,
            Authentication authentication) {
        
        CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
        
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Update allowed fields
        if (updateData.containsKey("firstName")) {
            user.setFirstName((String) updateData.get("firstName"));
        }
        if (updateData.containsKey("lastName")) {
            user.setLastName((String) updateData.get("lastName"));
        }
        if (updateData.containsKey("phoneNumber")) {
            user.setPhoneNumber((String) updateData.get("phoneNumber"));
        }
        if (updateData.containsKey("profilePictureUrl")) {
            user.setProfilePictureUrl((String) updateData.get("profilePictureUrl"));
        }
        
        User updatedUser = userRepository.save(user);
        UserResponse userResponse = userService.mapToUserResponse(updatedUser);
        
        return ResponseEntity.ok(userResponse);
    }
    
    @PutMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @RequestBody Map<String, String> passwordData,
            Authentication authentication) {
        
        CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
        
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        String currentPassword = passwordData.get("currentPassword");
        String newPassword = passwordData.get("newPassword");
        
        if (currentPassword == null || newPassword == null) {
            throw new RuntimeException("Current password and new password are required");
        }
        
        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }
        
        // Validate new password (you can add more validation here)
        if (newPassword.length() < 8) {
            throw new RuntimeException("New password must be at least 8 characters long");
        }
        
        // Hash and update password
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);
        
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }
}
