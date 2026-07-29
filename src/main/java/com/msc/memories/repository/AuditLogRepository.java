package com.msc.memories.repository;

import com.msc.memories.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    
    Page<AuditLog> findByRegistrationNumberContainingIgnoreCaseOrActionContainingIgnoreCaseOrDetailsContainingIgnoreCase(
            String regNo, String action, String details, Pageable pageable);
}