package com.msc.memories.controller;

import com.msc.memories.dto.ImageDto;
import com.msc.memories.model.AuditLog;
import com.msc.memories.model.Image;
import com.msc.memories.model.User;
import com.msc.memories.repository.ImageRepository;
import com.msc.memories.repository.UserRepository;
import com.msc.memories.service.AuditLogService;
import com.msc.memories.service.SystemLogService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final ImageRepository imageRepository;
    private final PasswordEncoder passwordEncoder;
    private final SystemLogService systemLogService;
    private final AuditLogService auditLogService;
    private final SessionRegistry sessionRegistry;

    public AdminController(UserRepository userRepository, SystemLogService systemLogService, ImageRepository imageRepository, PasswordEncoder passwordEncoder, AuditLogService auditLogService,SessionRegistry sessionRegistry) {
        this.userRepository = userRepository;
        this.imageRepository = imageRepository;
        this.passwordEncoder = passwordEncoder;
        this.systemLogService = systemLogService;
        this.auditLogService = auditLogService;
        this.sessionRegistry = sessionRegistry;
        
    }

    /**
     * Get Logged-in Admin Info for Header Badge
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, String>> getAdminInfo(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        
        User admin = userRepository.findByRegistrationNumber(userDetails.getUsername())
                .or(() -> userRepository.findByEmail(userDetails.getUsername()))
                .orElseThrow(() -> new RuntimeException("Admin user not found"));

        Map<String, String> response = new HashMap<>();
        response.put("name", admin.getName() != null ? admin.getName() : "Admin");
        response.put("email", admin.getEmail());
        response.put("registrationNumber", admin.getRegistrationNumber());
        return ResponseEntity.ok(response);
    }

    /**
     * Dashboard Summary Statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        // Count unique principals with non-expired sessions
        long activeSessionsCount = sessionRegistry.getAllPrincipals().stream()
                .filter(principal -> !sessionRegistry.getAllSessions(principal, false).isEmpty())
                .count();

        stats.put("totalUsers", userRepository.count());
        stats.put("activeUsers", activeSessionsCount); // Real-time session-based active count
        stats.put("totalPhotos", imageRepository.count());
        stats.put("status", "UP");

        return ResponseEntity.ok(stats);
    }

    /**
     * Get ALL Users
     */
    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> getAllUsers() {
        List<User> users = userRepository.findAll();

        List<Map<String, Object>> userList = users.stream()
                .map(u -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("registrationNumber", u.getRegistrationNumber());
                    map.put("name", u.getName());
                    map.put("email", u.getEmail());
                    map.put("phoneNumber", u.getPhoneNumber() != null ? u.getPhoneNumber() : "");
                    map.put("role", u.getRole() != null ? u.getRole() : "USER");
                    map.put("enabled", u.isEnabled());
                    return map;
                }).collect(Collectors.toList());

        return ResponseEntity.ok(userList);
    }

    /**
     * Get Non-Admin Users for Dropdown
     */
    @GetMapping("/users/students-only")
    public ResponseEntity<List<Map<String, Object>>> getNonAdminUsersForDropdown() {
        List<User> users = userRepository.findAll();

        List<Map<String, Object>> studentList = users.stream()
                .filter(u -> u.getRole() == null || 
                       (!u.getRole().equalsIgnoreCase("ADMIN") && !u.getRole().equalsIgnoreCase("ROLE_ADMIN")))
                .map(u -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("registrationNumber", u.getRegistrationNumber());
                    map.put("name", u.getName());
                    map.put("email", u.getEmail());
                    return map;
                }).collect(Collectors.toList());

        return ResponseEntity.ok(studentList);
    }

    /**
     * Create New User or Admin Account
     */
    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestBody Map<String, String> payload) {
        String regNo = payload.get("registrationNumber");
        String email = payload.get("email");

        if (regNo == null || regNo.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Registration Number is required."));
        }

        if (userRepository.existsById(regNo)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Registration Number already exists."));
        }

        if (userRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email address is already in use."));
        }

        User user = new User();
        user.setRegistrationNumber(regNo.trim());
        user.setName(payload.get("name"));
        user.setEmail(email.trim());
        user.setPhoneNumber(payload.get("phoneNumber"));
        user.setPassword(passwordEncoder.encode(payload.get("password")));
        
        String roleInput = payload.getOrDefault("role", "USER").toUpperCase();
        user.setRole(roleInput);
        user.setEnabled(true);

        userRepository.save(user);
        
        // Write audit record
        auditLogService.logActivity(regNo, payload.get("name"), "USER_CREATED", "Account created with role " + roleInput, "SYSTEM");
        
        return ResponseEntity.ok(Map.of("status", "success", "message", "Account created successfully as " + roleInput));
    }

    /**
     * Update Existing User Account
     */
    @PutMapping("/users/{regNo}")
    public ResponseEntity<?> updateUser(@PathVariable String regNo, @RequestBody Map<String, String> payload) {
        return userRepository.findById(regNo).map(user -> {
            user.setName(payload.get("name"));
            user.setEmail(payload.get("email"));
            user.setPhoneNumber(payload.get("phoneNumber"));
            
            if (payload.get("role") != null) {
                user.setRole(payload.get("role"));
            }
            if (payload.get("password") != null && !payload.get("password").trim().isEmpty()) {
                user.setPassword(passwordEncoder.encode(payload.get("password")));
            }

            userRepository.save(user);
            
            // Write audit record
            auditLogService.logActivity(regNo, user.getName(), "USER_UPDATED", "User account modified by Admin", "SYSTEM");
            
            return ResponseEntity.ok(Map.of("status", "success", "message", "User updated successfully."));
        }).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Fetch Images with Strict User Filter
     */
    @GetMapping("/images")
    public ResponseEntity<Page<ImageDto>> getAdminImages(
            @RequestParam(required = false) String regNo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size,
            @RequestParam(defaultValue = "") String search) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("uploadedAt").descending());
        Page<Image> imagePage;

        if (regNo != null && !regNo.trim().isEmpty()) {
            User user = userRepository.findById(regNo.trim())
                    .orElseThrow(() -> new RuntimeException("User not found with Registration Number: " + regNo));
            
            if (search != null && !search.trim().isEmpty()) {
                imagePage = imageRepository.findByUserAndFileNameContainingIgnoreCase(user, search.trim(), pageable);
            } else {
                imagePage = imageRepository.findByUser(user, pageable);
            }
        } else {
            if (search != null && !search.trim().isEmpty()) {
                imagePage = imageRepository.findByFileNameContainingIgnoreCase(search.trim(), pageable);
            } else {
                imagePage = imageRepository.findAll(pageable);
            }
        }

        return ResponseEntity.ok(imagePage.map(this::convertToDto));
    }

    /**
     * Admin Delete Image
     */
    @DeleteMapping("/images/{id}")
    public ResponseEntity<?> adminDeleteImage(@PathVariable Long id) {
        return imageRepository.findById(id).map(img -> {
            String uploader = img.getUser() != null ? img.getUser().getRegistrationNumber() : "UNKNOWN";
            imageRepository.delete(img);
            
            // Write audit record
            auditLogService.logActivity(uploader, "N/A", "IMAGE_DELETED", "Photo ID " + id + " deleted by admin", "SYSTEM");
            
            return ResponseEntity.ok(Map.of("status", "success", "message", "Image deleted successfully."));
        }).orElse(ResponseEntity.notFound().build());
    }

    private ImageDto convertToDto(Image img) {
        ImageDto dto = new ImageDto();
        dto.setId(img.getId());
        dto.setFileName(img.getFileName());
        dto.setImageUrl(img.getImageUrl());
        dto.setThumbnailUrl(img.getThumbnailUrl());
        dto.setPublicId(img.getPublicId());
        dto.setUploadedAt(img.getUploadedAt());
        return dto;
    }

    @GetMapping("/logs")
    public ResponseEntity<List<String>> fetchSystemLogs() {
        return ResponseEntity.ok(systemLogService.getRecentLogs());
    }

    // FIXED: Changed from "/api/admin/audit-logs" to "/audit-logs"
    @GetMapping("/audit-logs")
    public ResponseEntity<Page<AuditLog>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(defaultValue = "") String search) {
        return ResponseEntity.ok(auditLogService.getLogs(page, size, search));
    }
}