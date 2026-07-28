package com.msc.memories.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;

@RestController
public class ThumbnailController {

    @GetMapping("/thumbnail/**")
    public ResponseEntity<?> getThumbnail(HttpServletRequest request) {
        // Extract the full path after '/thumbnail/'
        String fullPath = request.getRequestURI();
        String publicId = fullPath.substring(fullPath.indexOf("/thumbnail/") + 11);

        // TODO: Fetch/stream the thumbnail byte array or redirect to Cloudinary/S3 URL
        // Example redirect to Cloudinary thumbnail:
        // String cdnUrl = "https://res.cloudinary.com/your-cloud-name/image/upload/c_thumb,w_300/" + publicId;
        // return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(cdnUrl)).build();

        return ResponseEntity.ok().build();
    }
}