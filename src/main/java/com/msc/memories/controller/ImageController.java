package com.msc.memories.controller;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.msc.memories.dto.ImageDto;
import com.msc.memories.model.Image;
import com.msc.memories.model.User;
import com.msc.memories.repository.ImageRepository;
import com.msc.memories.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    private final ImageRepository imageRepository;
    private final UserRepository userRepository;

    @Autowired(required = false)
    private Cloudinary cloudinary;

    public ImageController(ImageRepository imageRepository, UserRepository userRepository) {
        this.imageRepository = imageRepository;
        this.userRepository = userRepository;
    }

    /**
     * Public Gallery: Fetch all images from all users
     */
    @GetMapping
    public ResponseEntity<Page<ImageDto>> getAllImages(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size,
            @RequestParam(defaultValue = "") String search) {

        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("uploadedAt").descending());

        Page<Image> imagePage;
        if (search != null && !search.trim().isEmpty()) {
            imagePage = imageRepository.findByFileNameContainingIgnoreCase(search.trim(), pageable);
        } else {
            imagePage = imageRepository.findAll(pageable);
        }

        return ResponseEntity.ok(imagePage.map(this::convertToDto));
    }

    /**
     * My Uploads: Fetch ONLY images uploaded by the authenticated user
     */
    @GetMapping("/my-uploads")
    public ResponseEntity<Page<ImageDto>> getMyUploadedImages(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size,
            @RequestParam(defaultValue = "") String search) {

        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }

        User user = getCurrentUser(userDetails);
        Pageable pageable = PageRequest.of(page, size, Sort.by("uploadedAt").descending());

        Page<Image> imagePage;
        if (search != null && !search.trim().isEmpty()) {
            imagePage = imageRepository.findByUserAndFileNameContainingIgnoreCase(user, search.trim(), pageable);
        } else {
            imagePage = imageRepository.findByUser(user, pageable);
        }

        return ResponseEntity.ok(imagePage.map(this::convertToDto));
    }

    /**
     * Delete Image: Allows users to delete ONLY their own uploaded images
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteImage(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        }

        User user = getCurrentUser(userDetails);

        // Security check: Verify image belongs to logged-in user
        Image image = imageRepository.findByIdAndUser(id, user)
                .orElse(null);

        if (image == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Image not found or you do not have permission to delete it."));
        }

        // Delete from Cloudinary if publicId exists and Cloudinary bean is present
        if (cloudinary != null && image.getPublicId() != null && !image.getPublicId().isEmpty()) {
            try {
                cloudinary.uploader().destroy(image.getPublicId(), ObjectUtils.emptyMap());
            } catch (Exception e) {
                System.err.println("Failed to delete image from Cloudinary: " + e.getMessage());
            }
        }

        // Delete from Database
        imageRepository.delete(image);

        return ResponseEntity.ok(Map.of("status", "success", "message", "Image deleted successfully"));
    }

    // Helper methods
    private User getCurrentUser(UserDetails userDetails) {
        return userRepository.findByRegistrationNumber(userDetails.getUsername())
                .or(() -> userRepository.findByEmail(userDetails.getUsername()))
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private ImageDto convertToDto(Image img) {
        ImageDto dto = new ImageDto();
        dto.setId(img.getId());
        dto.setFileName(img.getFileName());
        dto.setImageUrl(img.getImageUrl());
        dto.setThumbnailUrl(img.getThumbnailUrl());
        dto.setPublicId(img.getPublicId());
        dto.setUploadedAt(img.getUploadedAt());
        
        // Populate uploader details safely from the JPA entity relationship
        if (img.getUser() != null) {
            dto.setUploaderName(img.getUser().getName());
            dto.setUploaderRegNo(img.getUser().getRegistrationNumber());
        } else {
            dto.setUploaderName("Unknown");
            dto.setUploaderRegNo("");
        }
        return dto;
    }
}