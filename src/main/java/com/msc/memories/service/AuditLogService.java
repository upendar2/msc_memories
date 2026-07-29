package com.msc.memories.service;

import com.msc.memories.model.AuditLog;
import com.msc.memories.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void logActivity(String regNo, String name, String action, String details, String ipAddress) {
        try {
            AuditLog log = new AuditLog(regNo, name, action, details, ipAddress);
            auditLogRepository.save(log);
        } catch (Exception e) {
            System.err.println("Failed to write audit log: " + e.getMessage());
        }
    }

    public Page<AuditLog> getLogs(int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        if (search != null && !search.trim().isEmpty()) {
            return auditLogRepository.findByRegistrationNumberContainingIgnoreCaseOrActionContainingIgnoreCaseOrDetailsContainingIgnoreCase(
                    search.trim(), search.trim(), search.trim(), pageable);
        }
        return auditLogRepository.findAll(pageable);
    }
}