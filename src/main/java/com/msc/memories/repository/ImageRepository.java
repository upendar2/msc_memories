package com.msc.memories.repository;

import com.msc.memories.model.Image;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {
    // Fetch all images for all students ordered by latest upload
    List<Image> findAllByOrderByUploadedAtDesc();

    // Fetch images uploaded by a specific student
    List<Image> findByUserRegistrationNumberOrderByUploadedAtDesc(String registrationNumber);

    Optional<Image> findByPublicId(String publicId);
    
 // Paginated search for user images by original filename
    Page<Image> findByFileNameContainingIgnoreCase(String fileName, Pageable pageable);
    
    // Default paginated fetch for user images
    Page<Image> findByUserRegistrationNumber(String  registrationNumber, Pageable pageable);
}