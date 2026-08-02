package com.msc.memories.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String registrationNumber;

    private String userName;

    @Column(nullable = false)
    private String action;

    @Column(length = 500)
    private String details;

    private String ipAddress;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    public AuditLog() {
        // Default to Indian Standard Time (IST)
        this.timestamp = ZonedDateTime.now(ZoneId.of("Asia/Kolkata")).toLocalDateTime();
    }

    public AuditLog(String registrationNumber, String userName, String action, String details, String ipAddress) {
        this.registrationNumber = registrationNumber;
        this.userName = userName;
        this.action = action;
        this.details = details;
        this.ipAddress = ipAddress;
        // Sets current Indian Standard Time (IST)
        this.timestamp = ZonedDateTime.now(ZoneId.of("Asia/Kolkata")).toLocalDateTime();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        if (timestamp != null) {
            this.timestamp = ZonedDateTime.now(ZoneId.of("Asia/Kolkata")).toLocalDateTime();
        }
    }
}