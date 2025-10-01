package com.healthapp.auth.service;

import com.healthapp.auth.dto.request.DoctorActivationRequestDto;
import com.healthapp.auth.dto.response.DoctorPendingResponse;
import com.healthapp.auth.entity.DoctorActivationRequest;
import com.healthapp.auth.entity.User;
import com.healthapp.auth.enums.UserRole;
import com.healthapp.auth.repository.DoctorActivationRequestRepository;
import com.healthapp.auth.repository.UserRepository;
import com.healthapp.auth.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class DoctorActivationService {
    
    private final DoctorActivationRequestRepository activationRequestRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

public void createActivationRequest(User doctor) {
    if (activationRequestRepository.findByDoctorId(doctor.getId()).isPresent()) {
        log.info("Activation request already exists for doctor: {}", doctor.getEmail());
        return;
    }
    
    DoctorActivationRequest request = DoctorActivationRequest.builder()
            .doctorId(doctor.getId())
            .doctorEmail(doctor.getEmail())
            .doctorFullName(doctor.getFullName())
            .medicalLicenseNumber(doctor.getMedicalLicenseNumber())
            .specialization(doctor.getSpecialization())
            .hospitalAffiliation(doctor.getHospitalAffiliation())
            .yearsOfExperience(doctor.getYearsOfExperience())
            .isPending(true)
            .requestedAt(LocalDateTime.now())
            .build();
    
    try {
        DoctorActivationRequest savedRequest = activationRequestRepository.save(request);
        log.info("Activation request saved with ID: {}, Doctor: {}", savedRequest.getId(), doctor.getEmail());
    } catch (Exception e) {
        log.error("Failed to save activation request for doctor: {}", doctor.getEmail(), e);
        throw new RuntimeException("Failed to create activation request", e);
    }
}

    
    public List<DoctorPendingResponse> getPendingDoctorRequests() {
        List<User> pendingDoctors = userRepository.findPendingDoctors();
        
        return pendingDoctors.stream()
                .map(this::mapToDoctorPendingResponse)
                .collect(Collectors.toList());
    }
    
    public void processDoctorActivationRequest(DoctorActivationRequestDto request, Authentication authentication) {
        CustomUserPrincipal admin = (CustomUserPrincipal) authentication.getPrincipal();
        log.info("Processing activation request for doctor ID: {}", request.getDoctorId());
        User doctor = userRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new RuntimeException("Doctor not found"));
        
        if (!doctor.hasRole(UserRole.DOCTOR)) {
            throw new RuntimeException("User is not a doctor");
        }
        
        DoctorActivationRequest activationRequest = activationRequestRepository.findByDoctorId(doctor.getId())
                .orElseThrow(() -> new RuntimeException("Activation request not found"));
        
        if ("APPROVE".equalsIgnoreCase(request.getAction())) {
            approveDoctorAccount(doctor, admin, activationRequest, request.getNotes());
        } else if ("REJECT".equalsIgnoreCase(request.getAction())) {
            rejectDoctorAccount(doctor, admin, activationRequest, request.getNotes());
        } else {
            throw new RuntimeException("Invalid action. Use APPROVE or REJECT");
        }
    }
    
    private void approveDoctorAccount(User doctor, CustomUserPrincipal admin, 
                                    DoctorActivationRequest activationRequest, String notes) {
        // Activate doctor account
        doctor.setIsActivated(true);
        doctor.setActivatedBy(admin.getId());
        doctor.setActivationDate(LocalDateTime.now());
        userRepository.save(doctor);
        
        // Mark activation request as processed
        activationRequest.markAsProcessed(admin.getId(), admin.getEmail(), notes);
        activationRequestRepository.save(activationRequest);
        
        // Send confirmation email to doctor
        emailService.sendDoctorActivationConfirmationEmail(doctor);
        
        log.info("Doctor account approved: {} by admin: {}", doctor.getEmail(), admin.getEmail());
    }
    
    private void rejectDoctorAccount(User doctor, CustomUserPrincipal admin,
                                   DoctorActivationRequest activationRequest, String notes) {
        // Mark activation request as processed (rejected)
        activationRequest.markAsProcessed(admin.getId(), admin.getEmail(), notes);
        activationRequestRepository.save(activationRequest);
        
        // Optionally delete the doctor account or keep it deactivated
        // For now, we'll keep it deactivated
        
        // Send rejection email to doctor
        emailService.sendDoctorActivationRejectionEmail(doctor, notes);
        
        log.info("Doctor account rejected: {} by admin: {}", doctor.getEmail(), admin.getEmail());
    }
    private DoctorPendingResponse mapToDoctorPendingResponse(User doctor) {
    // Get the activation request to get the ID
    DoctorActivationRequest activationRequest = activationRequestRepository
        .findByDoctorId(doctor.getId())
        .orElse(null);
    
    return DoctorPendingResponse.builder()
            .id(activationRequest != null ? activationRequest.getId() : null)
            .doctorId(doctor.getId())
            .email(doctor.getEmail())
            .fullName(doctor.getFullName())
            .medicalLicenseNumber(doctor.getMedicalLicenseNumber())
            .specialization(doctor.getSpecialization())
            .hospitalAffiliation(doctor.getHospitalAffiliation())
            .yearsOfExperience(doctor.getYearsOfExperience())
            .registrationDate(doctor.getCreatedAt())
            .activationRequestDate(doctor.getActivationRequestDate())
            .build();
}

    public long getPendingDoctorRequestsCount() {
        return activationRequestRepository.countByIsPendingTrue();
    }
}