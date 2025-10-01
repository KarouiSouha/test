package com.healthapp.auth.service;

import com.healthapp.auth.entity.User;
import com.healthapp.auth.enums.UserRole;
import com.healthapp.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class EmailService {
    
    private final JavaMailSender mailSender;
    private final UserRepository userRepository;
    
    @Value("${spring.mail.username:noreply@healthapp.com}")
    private String fromEmail;
    
    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;
    
    // Modified constructor to make JavaMailSender optional
    public EmailService(@Autowired(required = false) JavaMailSender mailSender, 
                       UserRepository userRepository) {
        this.mailSender = mailSender;
        this.userRepository = userRepository;
    }
    
    public void sendDoctorRegistrationNotificationToAdmin(User doctor) {
        if (mailSender == null) {
            log.warn("Email service not configured - skipping doctor registration notification for: {}", doctor.getEmail());
            return;
        }
        
        List<User> admins = userRepository.findByRolesContaining(UserRole.ADMIN);
        
        for (User admin : admins) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(fromEmail);
                message.setTo(admin.getEmail());
                message.setSubject("New Doctor Registration Pending Approval");
                message.setText(buildDoctorRegistrationNotificationContent(doctor, admin));
                
                mailSender.send(message);
                log.info("Doctor registration notification sent to admin: {}", admin.getEmail());
            } catch (Exception e) {
                log.error("Failed to send email to admin: {}", admin.getEmail(), e);
            }
        }
    }
    
    public void sendDoctorActivationConfirmationEmail(User doctor) {
        if (mailSender == null) {
            log.warn("Email service not configured - skipping activation confirmation for: {}", doctor.getEmail());
            return;
        }
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(doctor.getEmail());
            message.setSubject("Account Activated - Welcome to Health App");
            message.setText(buildDoctorActivationConfirmationContent(doctor));
            
            mailSender.send(message);
            log.info("Doctor activation confirmation sent to: {}", doctor.getEmail());
        } catch (Exception e) {
            log.error("Failed to send activation confirmation to doctor: {}", doctor.getEmail(), e);
        }
    }
    
    public void sendDoctorActivationRejectionEmail(User doctor, String reason) {
        if (mailSender == null) {
            log.warn("Email service not configured - skipping activation rejection for: {}", doctor.getEmail());
            return;
        }
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(doctor.getEmail());
            message.setSubject("Account Registration Review - Health App");
            message.setText(buildDoctorActivationRejectionContent(doctor, reason));
            
            mailSender.send(message);
            log.info("Doctor activation rejection sent to: {}", doctor.getEmail());
        } catch (Exception e) {
            log.error("Failed to send activation rejection to doctor: {}", doctor.getEmail(), e);
        }
    }
    
    private String buildDoctorRegistrationNotificationContent(User doctor, User admin) {
        return String.format(
            "Dear %s,\n\n" +
            "A new doctor has registered on the Health App platform and requires your approval.\n\n" +
            "Doctor Details:\n" +
            "- Name: %s\n" +
            "- Email: %s\n" +
            "- Medical License: %s\n" +
            "- Specialization: %s\n" +
            "- Hospital: %s\n" +
            "- Experience: %d years\n" +
            "- Registration Date: %s\n\n" +
            "Please review this application in the admin dashboard:\n" +
            "%s/admin/doctor-approvals\n\n" +
            "Best regards,\n" +
            "Health App Team",
            admin.getFirstName(),
            doctor.getFullName(),
            doctor.getEmail(),
            doctor.getMedicalLicenseNumber(),
            doctor.getSpecialization(),
            doctor.getHospitalAffiliation(),
            doctor.getYearsOfExperience(),
            doctor.getCreatedAt().toString(),
            frontendUrl
        );
    }
    
    private String buildDoctorActivationConfirmationContent(User doctor) {
        return String.format(
            "Dear Dr. %s,\n\n" +
            "Congratulations! Your doctor account on Health App has been successfully activated.\n\n" +
            "You can now access all doctor features including:\n" +
            "- Patient health monitoring\n" +
            "- Medical consultation tools\n" +
            "- Health analytics dashboard\n" +
            "- Prescription management\n\n" +
            "Login to your account: %s/login\n\n" +
            "If you have any questions, please don't hesitate to contact our support team.\n\n" +
            "Best regards,\n" +
            "Health App Team",
            doctor.getLastName(),
            frontendUrl
        );
    }
    
    private String buildDoctorActivationRejectionContent(User doctor, String reason) {
        return String.format(
            "Dear Dr. %s,\n\n" +
            "Thank you for your interest in joining Health App as a verified doctor.\n\n" +
            "After careful review, we were unable to approve your doctor account at this time.\n\n" +
            "Reason: %s\n\n" +
            "If you believe this decision was made in error or if you have additional " +
            "documentation to support your application, please contact our support team " +
            "at support@healthapp.com.\n\n" +
            "You are welcome to reapply once any issues have been resolved.\n\n" +
            "Best regards,\n" +
            "Health App Team",
            doctor.getLastName(),
            reason != null ? reason : "Your credentials could not be verified at this time."
        );
    }
}