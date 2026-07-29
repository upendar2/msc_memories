package com.msc.memories.service;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class EmailService {

    private final SendGrid sendGrid;

    @Value("${sendgrid.from-email}")
    private String fromEmail;

    @Value("${sendgrid.from-name:MSc Memories}")
    private String fromName;

    public EmailService(@Value("${sendgrid.api-key}") String apiKey) {
        this.sendGrid = new SendGrid(apiKey);
    }

    /**
     * 1. Send Simple Text Email
     */
    public void sendSimpleEmail(String toEmail, String subject, String body) {
        Email from = new Email(fromEmail, fromName);
        Email to = new Email(toEmail);
        Content content = new Content("text/plain", body);
        Mail mail = new Mail(from, subject, to, content);

        sendMail(mail);
    }

    /**
     * 2. Send Password Reset OTP Email (HTML Template)
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
     * 4. Send Custom HTML Email
     */
    public void sendHtmlEmail(String toEmail, String subject, String htmlBody) {
        Email from = new Email(fromEmail, fromName);
        Email to = new Email(toEmail);
        Content content = new Content("text/html", htmlBody);
        Mail mail = new Mail(from, subject, to, content);

        sendMail(mail);
    }

    /**
     * Helper to execute API HTTP requests to SendGrid
     */
    private void sendMail(Mail mail) {
        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = this.sendGrid.api(request);
            int statusCode = response.getStatusCode();

            if (statusCode >= 200 && statusCode < 300) {
                System.out.println("SendGrid Email sent successfully! Status code: " + statusCode);
            } else {
                System.err.println("Failed to send email via SendGrid. Status code: " + statusCode + ", Body: " + response.getBody());
                throw new RuntimeException("SendGrid Email dispatch failed with HTTP code " + statusCode);
            }
        } catch (IOException e) {
            System.err.println("IO Exception while communicating with SendGrid API: " + e.getMessage());
            throw new RuntimeException("Failed to send email via SendGrid API", e);
        }
    }
}