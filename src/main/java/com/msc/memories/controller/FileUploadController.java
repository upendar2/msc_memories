package com.msc.memories.controller;

import com.msc.memories.model.Image;
import com.msc.memories.model.User;
import com.msc.memories.repository.ImageRepository;
import com.msc.memories.repository.UserRepository;
import com.msc.memories.service.CloudinaryService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
public class FileUploadController {

    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;
    private final ImageRepository imageRepo;

    public FileUploadController(UserRepository userRepository, 
                                CloudinaryService cloudinaryService, 
                                ImageRepository imageRepo) {
        this.userRepository = userRepository;
        this.cloudinaryService = cloudinaryService;
        this.imageRepo = imageRepo;
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {

        Map<String, Object> response = new HashMap<>();

        if (file == null || file.isEmpty()) {
            response.put("status", "error");
            response.put("message", "File is empty");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            // 1. Fetch authenticated user
            User user = userRepository.findByRegistrationNumber(userDetails.getUsername())
                    .or(() -> userRepository.findByEmail(userDetails.getUsername()))
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // 2. Upload to Cloudinary under the student's registration number folder
            Map<String, Object> uploadResult = cloudinaryService.uploadStudentMemory(file, user.getRegistrationNumber());

            String publicId = (String) uploadResult.get("public_id");
            String secureUrl = (String) uploadResult.get("secure_url");
            
            // Generate optimized square thumbnail URL using the public_id
            String thumbnailUrl = cloudinaryService.getThumbnailUrl(publicId, 400, 400);

            // Extract image dimensions & metadata from Cloudinary result
//            Integer width = (Integer) uploadResult.get("width");
//            Integer height = (Integer) uploadResult.get("height");
//            Long size = file.getSize();

            // 3. Save Image Entity to Database
            Image image = new Image();
            image.setFileName(file.getOriginalFilename());
            image.setPublicId(publicId);
            image.setImageUrl(secureUrl);
            image.setThumbnailUrl(thumbnailUrl);
//            image.setSize(size);
//            if (width != null) image.setWidth(width);
//            if (height != null) image.setHeight(height);
            image.setUploadedAt(LocalDateTime.now());
            image.setUser(user);

            imageRepo.save(image);

            // 4. Return success response
            response.put("status", "success");
            response.put("message", "File uploaded successfully");
            response.put("publicId", publicId);
            response.put("imageUrl", secureUrl);
            response.put("thumbnailUrl", thumbnailUrl);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("status", "error");
            response.put("message", "Upload failed: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}