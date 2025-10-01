package com.healthapp.auth.controller;

import com.healthapp.auth.dto.request.DoctorActivationRequestDto;
import com.healthapp.auth.dto.response.DoctorPendingResponse;
import com.healthapp.auth.service.DoctorActivationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminController {
    
    private final DoctorActivationService doctorActivationService;
    
    @GetMapping("/doctors/pending")
    public ResponseEntity<List<DoctorPendingResponse>> getPendingDoctors() {
        log.info("Admin requesting pending doctors list");
        List<DoctorPendingResponse> pendingDoctors = doctorActivationService.getPendingDoctorRequests();
        return ResponseEntity.ok(pendingDoctors);
    }
    
    @PostMapping("/doctors/activate")
    public ResponseEntity<Map<String, String>> activateDoctor(
            @Valid @RequestBody DoctorActivationRequestDto request,
            Authentication authentication) {
        
        log.info("Admin processing doctor activation request for doctor ID: {}", request.getDoctorId());
        doctorActivationService.processDoctorActivationRequest(request, authentication);
        
        String message = "APPROVE".equalsIgnoreCase(request.getAction()) 
            ? "Doctor account has been successfully activated" 
            : "Doctor account activation has been rejected";
            
        return ResponseEntity.ok(Map.of("message", message));
    }
    
    @GetMapping("/doctors/pending/count")
    public ResponseEntity<Map<String, Long>> getPendingDoctorsCount() {
        long count = doctorActivationService.getPendingDoctorRequestsCount();
        return ResponseEntity.ok(Map.of("count", count));
    }
}

