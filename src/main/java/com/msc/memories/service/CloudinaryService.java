package com.msc.memories.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;
    private final String defaultFolder = "msc_student_memories";

    // Inject credentials from application.properties
    public CloudinaryService(
            @Value("${cloudinary.cloud-name}") String cloudName,
            @Value("${cloudinary.api-key}") String apiKey,
            @Value("${cloudinary.api-secret}") String apiSecret) {

        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
    }

    // ==========================================
    // 1. INSERT / UPLOAD OPERATIONS
    // ==========================================

    /**
     * Uploads a file to Cloudinary under the default folder.
     * @return Map containing upload metadata (secure_url, public_id, format, etc.)
     */
    public Map<String, Object> uploadImage(MultipartFile file) throws IOException {
        return uploadImageToFolder(file, defaultFolder, null);
    }

    /**
     * Uploads an image attached to a specific student's Registration Number folder with tags.
     */
    public Map<String, Object> uploadStudentMemory(MultipartFile file, String registrationNumber) throws IOException {
        String studentFolder = defaultFolder + "/" + registrationNumber;
        return uploadImageToFolder(file, studentFolder, registrationNumber);
    }

    /**
     * Helper Method: Flexible upload specifying custom folder and tag.
     */
    public Map<String, Object> uploadImageToFolder(MultipartFile file, String folder, String tag) throws IOException {
        Map<String, Object> params = ObjectUtils.asMap(
                "folder", folder,
                "resource_type", "image",
                "use_filename", true,
                "unique_filename", true
        );

        if (tag != null && !tag.isBlank()) {
            params.put("tags", tag);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(), params);
        return uploadResult;
    }

    // ==========================================
    // 2. FETCH / READ / URL GENERATION
    // ==========================================

    /**
     * Generates an optimized web display URL (auto-format like WebP/AVIF and auto-quality).
     */
    public String getOptimizedUrl(String publicId) {
        return cloudinary.url()
                .transformation(new com.cloudinary.Transformation()
                        .quality("auto")
                        .fetchFormat("auto"))
                .generate(publicId);
    }

    /**
     * Generates a square cropped thumbnail URL ideal for gallery grid views.
     */
    public String getThumbnailUrl(String publicId, int width, int height) {
        return cloudinary.url()
                .transformation(new com.cloudinary.Transformation()
                        .width(width)
                        .height(height)
                        .crop("fill")
                        .gravity("auto")
                        .quality("auto")
                        .fetchFormat("auto"))
                .generate(publicId);
    }

    /**
     * Searches and lists images tagged with a specific Registration Number.
     */
    public List<Map<String, Object>> fetchImagesByStudentTag(String registrationNumber) throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, Object> searchResult = cloudinary.search()
                .expression("tags:" + registrationNumber)
                .maxResults(100)
                .execute();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> resources = (List<Map<String, Object>>) searchResult.get("resources");
        return resources != null ? resources : Collections.emptyList();
    }

    // ==========================================
    // 3. UPDATE / OVERWRITE OPERATIONS
    // ==========================================

    /**
     * Replaces an existing image while preserving the exact original public_id.
     */
    public Map<String, Object> replaceExistingImage(MultipartFile newFile, String existingPublicId) throws IOException {
        Map<String, Object> params = ObjectUtils.asMap(
                "public_id", existingPublicId,
                "overwrite", true,
                "invalidate", true // Clears cached versions on Cloudinary CDN
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> result = cloudinary.uploader().upload(newFile.getBytes(), params);
        return result;
    }

    // ==========================================
    // 4. DELETE / DESTROY OPERATIONS
    // ==========================================

    /**
     * Deletes a single image using its unique Cloudinary public_id.
     * @return boolean true if successfully destroyed
     */
    public boolean deleteImage(String publicId) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().destroy(
                    publicId,
                    ObjectUtils.asMap("invalidate", true)
            );
            return "ok".equalsIgnoreCase((String) result.get("result"));
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Deletes multiple images at once using a list of public_ids.
     */
    public Map<String, Object> deleteMultipleImages(List<String> publicIds) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.api().deleteResources(
                    publicIds,
                    ObjectUtils.asMap("invalidate", true)
            );
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyMap();
        }
    }
}