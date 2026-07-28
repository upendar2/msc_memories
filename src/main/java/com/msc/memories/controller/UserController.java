package com.msc.memories.controller;

import com.msc.memories.dto.ImageDto;
import com.msc.memories.dto.UserResponseDto;
import com.msc.memories.model.User;
import com.msc.memories.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        User user = userRepository.findByRegistrationNumber(userDetails.getUsername())
                .or(() -> userRepository.findByEmail(userDetails.getUsername()))
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Map Entity to DTO
        UserResponseDto dto = new UserResponseDto();
        dto.setEmail(user.getEmail());
        dto.setName(user.getName());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setRegistrationNumber(user.getRegistrationNumber());
        dto.setEnabled(user.isEnabled());
        
        if (user.getImages() != null) {
            dto.setImages(user.getImages().stream().map(img -> {
                ImageDto imageDto = new ImageDto();
                imageDto.setId(img.getId());
                imageDto.setImageUrl(img.getImageUrl());
                imageDto.setThumbnailUrl(img.getThumbnailUrl());
                imageDto.setFileName(img.getFileName());
                imageDto.setPublicId(img.getPublicId());
//                imageDto.setSize(img.getSize());
//                imageDto.setWidth(img.getWidth());
//                imageDto.setHeight(img.getHeight());
                imageDto.setUploadedAt(img.getUploadedAt());
                return imageDto;
            }).collect(Collectors.toList()));
        }

        return ResponseEntity.ok(dto);
    }
}