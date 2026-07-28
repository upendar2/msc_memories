package com.msc.memories.repository;

import com.msc.memories.model.PasswordResetOtp;
import com.msc.memories.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetOtpRepository extends JpaRepository<PasswordResetOtp, Long> {

    // Fetch latest unused, unexpired OTP for a user
    Optional<PasswordResetOtp> findTopByUserAndOtpCodeAndUsedFalseAndExpiryTimeAfterOrderByCreatedAtDesc(
            User user, String otpCode, LocalDateTime now
    );

    // Invalidate existing active OTPs when a new request is made
    @Transactional
    @Modifying
    @Query("UPDATE PasswordResetOtp o SET o.used = true WHERE o.user = :user AND o.used = false")
    void invalidatePreviousOtps(User user);
    Optional<PasswordResetOtp>findTopByUserAndUsedFalseOrderByCreatedAtDesc(User user);
}