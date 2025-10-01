package com.healthapp.auth.service;

import com.healthapp.auth.dto.response.UserResponse;
import com.healthapp.auth.entity.User;
import com.healthapp.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    
    public UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .birthDate(user.getBirthDate())
                .gender(user.getGender())
                .phoneNumber(user.getPhoneNumber())
                .profilePictureUrl(user.getProfilePictureUrl())
                .roles(user.getRoles())
                .accountStatus(user.getAccountStatus())
                .isEmailVerified(user.getIsEmailVerified())
                .isActivated(user.getIsActivated())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .medicalLicenseNumber(user.getMedicalLicenseNumber())
                .specialization(user.getSpecialization())
                .hospitalAffiliation(user.getHospitalAffiliation())
                .yearsOfExperience(user.getYearsOfExperience())
                .activationDate(user.getActivationDate())
                .build();
    }
    
    public List<UserResponse> mapToUserResponseList(List<User> users) {
        return users.stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }
}
