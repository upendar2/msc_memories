package com.msc.memories.controller;

import java.util.Map;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.msc.memories.model.PasswordResetOtp;
import com.msc.memories.model.User;
import com.msc.memories.repository.PasswordResetOtpRepository;
import com.msc.memories.repository.UserRepository;
import com.msc.memories.service.EmailService;

@Controller
public class ForgotPasswordController {

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private PasswordResetOtpRepository otpRepo;

    @Autowired
    private PasswordEncoder passwordEncoder; // Injected BCryptPasswordEncoder

    private final EmailService emailService;

    public ForgotPasswordController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping("/generate-otp")
    public ResponseEntity<?> generateOtp(@RequestParam String registrationNumber, @RequestParam String email) {

        // 1. Verify user exists
        User user = userRepo.findByRegistrationNumberAndEmail(registrationNumber, email)
                .orElseThrow(() -> new RuntimeException("Invalid Registration Number or Email combination."));

        // 2. Invalidate older active OTPs for this user
        otpRepo.invalidatePreviousOtps(user);

        // 3. Generate raw 6-digit OTP
        String rawOtpCode = String.format("%06d", new Random().nextInt(900000) + 100000);

        // 4. Hash the OTP using BCrypt before storing
        String hashedOtpCode = passwordEncoder.encode(rawOtpCode);

        // 5. Save entity with the HASHED OTP
        PasswordResetOtp otpEntity = new PasswordResetOtp(hashedOtpCode, user, 10);
        otpRepo.save(otpEntity);

        // 6. Send the RAW unhashed OTP to the user's email
        emailService.sendPasswordResetOtp(user.getEmail(), user.getName(), rawOtpCode);

        return ResponseEntity.ok(Map.of("status", "success", "message", "OTP sent to registered email."));
    }
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestParam String registrationNumber, 
                                       @RequestParam String enteredOtp,
                                       @RequestParam String newPassword) {

        User user = userRepo.findByRegistrationNumber(registrationNumber)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Retrieve active unused OTP entity for this user
        PasswordResetOtp activeOtp = otpRepo.findTopByUserAndUsedFalseOrderByCreatedAtDesc(user)
                .orElseThrow(() -> new RuntimeException("No active OTP request found"));

        if (activeOtp.isExpired()) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "OTP has expired."));
        }

        // Compare raw entered OTP with stored BCrypt hash
        if (!passwordEncoder.matches(enteredOtp, activeOtp.getOtpCode())) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Invalid OTP."));
        }

        // Mark OTP as used
        activeOtp.setUsed(true);
        otpRepo.save(activeOtp);

        // Update user's password with BCrypt
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepo.save(user);

        return ResponseEntity.ok(Map.of("status", "success", "message", "Password updated successfully."));
    }
}