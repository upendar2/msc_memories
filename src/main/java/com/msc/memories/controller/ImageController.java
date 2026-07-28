package com.msc.memories.controller;

import com.msc.memories.dto.ImageDto;
import com.msc.memories.model.Image;
import com.msc.memories.model.User;
import com.msc.memories.repository.ImageRepository;
import com.msc.memories.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    private final ImageRepository imageRepository;
    private final UserRepository userRepository;

    public ImageController(ImageRepository imageRepository, UserRepository userRepository) {
        this.imageRepository = imageRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<Page<ImageDto>> getUserImages(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size,
            @RequestParam(defaultValue = "") String search) {

        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }

        User user = userRepository.findByRegistrationNumber(userDetails.getUsername())
                .or(() -> userRepository.findByEmail(userDetails.getUsername()))
                .orElseThrow(() -> new RuntimeException("User not found"));

        Pageable pageable = PageRequest.of(page, size, Sort.by("uploadedAt").descending());

        Page<Image> imagePage;
        if (search != null && !search.trim().isEmpty()) {
            imagePage = imageRepository.findByFileNameContainingIgnoreCase(search.trim(), pageable);
        } else {
            imagePage = imageRepository.findAll(pageable);
        }

        Page<ImageDto> dtoPage = imagePage.map(img -> {
            ImageDto imageDto = new ImageDto();
            imageDto.setId(img.getId());
            imageDto.setFileName(img.getFileName());
            imageDto.setImageUrl(img.getImageUrl());
            imageDto.setThumbnailUrl(img.getThumbnailUrl());
            imageDto.setPublicId(img.getPublicId());
            imageDto.setUploadedAt(img.getUploadedAt());
            return imageDto;
        });

        return ResponseEntity.ok(dtoPage);
    }
}