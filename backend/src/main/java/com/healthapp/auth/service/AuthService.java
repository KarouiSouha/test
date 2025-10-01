package com.healthapp.auth.service;

import com.healthapp.auth.dto.request.LoginRequest;
import com.healthapp.auth.dto.request.RefreshTokenRequest;
import com.healthapp.auth.dto.request.RegisterRequest;
import com.healthapp.auth.dto.response.AuthResponse;
import com.healthapp.auth.dto.response.UserResponse;
import com.healthapp.auth.entity.RefreshToken;
import com.healthapp.auth.entity.User;
import com.healthapp.auth.enums.UserRole;
import com.healthapp.auth.exception.InvalidTokenException;
import com.healthapp.auth.exception.UserAlreadyExistsException;
import com.healthapp.auth.repository.RefreshTokenRepository;
import com.healthapp.auth.repository.UserRepository;
import com.healthapp.auth.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuthService {
    
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final EmailService emailService;
    private final DoctorActivationService doctorActivationService; // Add this dependency
    public AuthResponse register(RegisterRequest request) {
        log.info("Attempting to register user with email: {}", request.getEmail());
        
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("User already exists with email: " + request.getEmail());
        }
        
        User user = buildUserFromRequest(request);
        User savedUser = userRepository.save(user);
        
        log.info("User registered successfully: {} with role: {}", savedUser.getEmail(), request.getRole());
        
        // Temporarily comment out doctor activation logic until DoctorActivationService is ready
    if (request.getRole() == UserRole.DOCTOR) {
        doctorActivationService.createActivationRequest(savedUser); // Explicitly create activation request
        emailService.sendDoctorRegistrationNotificationToAdmin(savedUser);
        log.info("Doctor registration notification sent for user: {}", savedUser.getEmail());
    }
        
        String accessToken = jwtService.generateAccessToken(savedUser);
        RefreshToken refreshToken = createRefreshToken(savedUser);
        
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .expiresIn(jwtService.getAccessTokenExpiration() / 1000)
                .user(userService.mapToUserResponse(savedUser))
                .build();
    }
    
    public AuthResponse login(LoginRequest request) {
        log.info("Attempting login for email: {}", request.getEmail());
        
        // Check if user exists BEFORE attempting authentication
        if (!userRepository.existsByEmail(request.getEmail())) {
            log.warn("Login attempt with non-existent email: {}", request.getEmail());
            throw new UsernameNotFoundException("No account found with this email address");
        }
        
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
            
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            user.resetFailedLoginAttempts();
            userRepository.save(user);
            
            String accessToken = jwtService.generateAccessToken(user);
            RefreshToken refreshToken = createRefreshToken(user);
            
            log.info("User logged in successfully: {}", user.getEmail());
            
            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken.getToken())
                    .expiresIn(jwtService.getAccessTokenExpiration() / 1000)
                    .user(userService.mapToUserResponse(user))
                    .build();
                    
        } catch (AuthenticationException e) {
            User user = userRepository.findByEmail(request.getEmail()).orElse(null);
            if (user != null) {
                user.incrementFailedLoginAttempts();
                userRepository.save(user);
            }
            log.warn("Authentication failed for email: {}", request.getEmail());
            throw e;
        }
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));
        
        if (!refreshToken.isValid()) {
            refreshTokenRepository.delete(refreshToken);
            throw new InvalidTokenException("Refresh token expired or revoked");
        }
        
        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Revoke old token and create new one
        refreshToken.revoke();
        refreshTokenRepository.save(refreshToken);
        
        String newAccessToken = jwtService.generateAccessToken(user);
        RefreshToken newRefreshToken = createRefreshToken(user);
        
        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken.getToken())
                .expiresIn(jwtService.getAccessTokenExpiration() / 1000)
                .user(userService.mapToUserResponse(user))
                .build();
    }
    
    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken)
                .ifPresent(token -> {
                    token.revoke();
                    refreshTokenRepository.save(token);
                });
    }
    
    private User buildUserFromRequest(RegisterRequest request) {
        User.UserBuilder userBuilder = User.builder()
                .email(request.getEmail().toLowerCase().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .birthDate(request.getBirthDate())
                .gender(request.getGender())
                .phoneNumber(request.getPhoneNumber())
                .roles(Set.of(request.getRole()));
        
        // Set isActivated based on role
        if (request.getRole() == UserRole.DOCTOR) {
            userBuilder.isActivated(false); // Doctors need admin approval
            userBuilder.medicalLicenseNumber(request.getMedicalLicenseNumber())
                    .specialization(request.getSpecialization())
                    .hospitalAffiliation(request.getHospitalAffiliation())
                    .yearsOfExperience(request.getYearsOfExperience())
                    .activationRequestDate(LocalDateTime.now());
        } else {
            userBuilder.isActivated(true); // Users are activated by default
        }
        
        return userBuilder.build();
    }
    
    private RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = RefreshToken.builder()
                .userId(user.getId())
                .token(jwtService.generateRefreshToken(user))
                .expiryDate(LocalDateTime.now().plusDays(7))
                .build();
        
        return refreshTokenRepository.save(refreshToken);
    }
}
