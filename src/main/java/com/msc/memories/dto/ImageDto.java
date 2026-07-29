package com.msc.memories.dto;

import java.time.LocalDateTime;

public class ImageDto {
    private Long id;
    private String imageUrl;
    private String thumbnailUrl;
    private String fileName;
    private String publicId;
    private LocalDateTime uploadedAt;
    
    // Uploader details
    private String uploaderName;
    private String uploaderRegNo;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getPublicId() { return publicId; }
    public void setPublicId(String publicId) { this.publicId = publicId; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }

    public String getUploaderName() { return uploaderName; }
    public void setUploaderName(String uploaderName) { this.uploaderName = uploaderName; }

    public String getUploaderRegNo() { return uploaderRegNo; }
    public void setUploaderRegNo(String uploaderRegNo) { this.uploaderRegNo = uploaderRegNo; }
}