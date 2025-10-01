package com.healthapp.auth.controller;

import com.healthapp.auth.dto.response.UserResponse;
import com.healthapp.auth.entity.User;
import com.healthapp.auth.repository.UserRepository;
import com.healthapp.auth.security.CustomUserPrincipal;
import com.healthapp.auth.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/doctor")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
@Slf4j
public class DoctorController {
    
    private final UserRepository userRepository;
    private final UserService userService;
    
    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getDoctorProfile(Authentication authentication) {
        CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
        
        User doctor = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("Doctor not found"));
        
        if (!doctor.isDoctor()) {
            throw new RuntimeException("User is not a doctor");
        }
        
        UserResponse doctorResponse = userService.mapToUserResponse(doctor);
        return ResponseEntity.ok(doctorResponse);
    }
    
    @PutMapping("/profile")
    public ResponseEntity<UserResponse> updateDoctorProfile(
            @RequestBody Map<String, Object> updateData,
            Authentication authentication) {
        
        CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
        
        User doctor = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("Doctor not found"));
        
        if (!doctor.isDoctor()) {
            throw new RuntimeException("User is not a doctor");
        }
        
        // Update allowed fields
        if (updateData.containsKey("firstName")) {
            doctor.setFirstName((String) updateData.get("firstName"));
        }
        if (updateData.containsKey("lastName")) {
            doctor.setLastName((String) updateData.get("lastName"));
        }
        if (updateData.containsKey("phoneNumber")) {
            doctor.setPhoneNumber((String) updateData.get("phoneNumber"));
        }
        if (updateData.containsKey("specialization")) {
            doctor.setSpecialization((String) updateData.get("specialization"));
        }
        if (updateData.containsKey("hospitalAffiliation")) {
            doctor.setHospitalAffiliation((String) updateData.get("hospitalAffiliation"));
        }
        if (updateData.containsKey("yearsOfExperience")) {
            doctor.setYearsOfExperience((Integer) updateData.get("yearsOfExperience"));
        }
        
        User updatedDoctor = userRepository.save(doctor);
        UserResponse doctorResponse = userService.mapToUserResponse(updatedDoctor);
        
        log.info("Doctor profile updated: {}", doctor.getEmail());
        return ResponseEntity.ok(doctorResponse);
    }
    
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDoctorDashboard(Authentication authentication) {
        CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
        
        User doctor = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("Doctor not found"));
        
        if (!doctor.isDoctor() || !doctor.getIsActivated()) {
            throw new RuntimeException("Doctor account is not activated");
        }
        
        // This would typically include patient statistics, appointments, etc.
        Map<String, Object> dashboardData = Map.of(
            "doctorInfo", userService.mapToUserResponse(doctor),
            "activationStatus", "ACTIVATED",
            "activationDate", doctor.getActivationDate(),
            "message", "Welcome to your doctor dashboard!"
        );
        
        return ResponseEntity.ok(dashboardData);
    }
    
    @GetMapping("/activation-status")
    public ResponseEntity<Map<String, Object>> getActivationStatus(Authentication authentication) {
        CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
        
        User doctor = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("Doctor not found"));
        
        if (!doctor.isDoctor()) {
            throw new RuntimeException("User is not a doctor");
        }
        
        String status = doctor.getIsActivated() ? "ACTIVATED" : "PENDING_ACTIVATION";
        String message = doctor.getIsActivated() 
            ? "Your account is activated and ready to use"
            : "Your account is pending admin approval. You will receive an email once approved.";
        
        Map<String, Object> statusData = Map.of(
            "isActivated", doctor.getIsActivated(),
            "status", status,
            "message", message,
            "activationRequestDate", doctor.getActivationRequestDate(),
            "activationDate", doctor.getActivationDate() != null ? doctor.getActivationDate() : "Not activated yet"
        );
        
        return ResponseEntity.ok(statusData);
    }
}

