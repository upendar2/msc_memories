package com.msc.memories.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * 1. Send Simple Text Email
     */
    public void sendSimpleEmail(String toEmail, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);
    }

    /**
     * 2. Send Password Reset OTP Email (HTML Template matching app theme)
     */
    public void sendPasswordResetOtp(String toEmail, String studentName, String otp) {
        String subject = "Password Reset OTP - MSc Memories";
        
        String htmlContent = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #0f172a; color: #ffffff; padding: 20px; }
                    .card { max-width: 480px; margin: 0 auto; background: #1e293b; border: 1px solid #334155; border-radius: 12px; padding: 30px; text-align: center; }
                    .logo { color: #FFD700; font-size: 22px; font-weight: bold; margin-bottom: 20px; }
                    .otp-box { background: #0f172a; border: 1px dashed #FFD700; color: #FFD700; font-size: 32px; font-weight: bold; letter-spacing: 6px; padding: 15px; border-radius: 8px; margin: 25px 0; }
                    .text-muted { color: #94a3b8; font-size: 14px; line-height: 1.5; }
                    .footer { margin-top: 25px; font-size: 12px; color: #64748b; }
                </style>
            </head>
            <body>
                <div class="card">
                    <div class="logo">📸 MSc Memories</div>
                    <p style="font-size: 16px;">Hello <strong>%s</strong>,</p>
                    <p class="text-muted">You requested a password reset for your account. Use the OTP code below to set a new password:</p>
                    
                    <div class="otp-box">%s</div>
                    
                    <p class="text-muted">This code will expire in <strong>10 minutes</strong>. If you did not request this, please ignore this email.</p>
                    
                    <div class="footer">
                        &copy; 2026 MSc Memories Portal. All rights reserved.
                    </div>
                </div>
            </body>
            </html>
            """, studentName, otp);

        sendHtmlEmail(toEmail, subject, htmlContent);
    }

    /**
     * 3. Send Password Reset Link Email (HTML)
     */
    public void sendPasswordResetLink(String toEmail, String studentName, String resetUrl) {
        String subject = "Reset Your Password - MSc Memories";

        String htmlContent = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #0f172a; color: #ffffff; padding: 20px; }
                    .card { max-width: 480px; margin: 0 auto; background: #1e293b; border: 1px solid #334155; border-radius: 12px; padding: 30px; text-align: center; }
                    .logo { color: #FFD700; font-size: 22px; font-weight: bold; margin-bottom: 20px; }
                    .btn { display: inline-block; background-color: #FFD700; color: #000000; text-decoration: none; padding: 12px 28px; font-weight: bold; border-radius: 8px; margin: 20px 0; }
                    .text-muted { color: #94a3b8; font-size: 14px; line-height: 1.5; }
                    .footer { margin-top: 25px; font-size: 12px; color: #64748b; }
                </style>
            </head>
            <body>
                <div class="card">
                    <div class="logo">📸 MSc Memories</div>
                    <p style="font-size: 16px;">Hello <strong>%s</strong>,</p>
                    <p class="text-muted">Click the button below to reset your password for MSc Memories:</p>
                    
                    <a href="%s" class="btn">Reset Password</a>
                    
                    <p class="text-muted">Or copy this link into your browser:<br><span style="color: #FFD700; word-break: break-all;">%s</span></p>
                    
                    <div class="footer">
                        &copy; 2026 MSc Memories Portal. All rights reserved.
                    </div>
                </div>
            </body>
            </html>
            """, studentName, resetUrl, resetUrl);

        sendHtmlEmail(toEmail, subject, htmlContent);
    }

    /**
     * 4. Helper method to construct and send MIME HTML emails
     */
    public void sendHtmlEmail(String toEmail, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true = HTML content

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send HTML email to " + toEmail, e);
        }
    }
}