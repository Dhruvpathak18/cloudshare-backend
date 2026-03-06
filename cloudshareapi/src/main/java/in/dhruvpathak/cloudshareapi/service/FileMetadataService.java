package in.dhruvpathak.cloudshareapi.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import in.dhruvpathak.cloudshareapi.document.FileMetadataDocument;
import in.dhruvpathak.cloudshareapi.document.ProfileDocument;
import in.dhruvpathak.cloudshareapi.dto.FileMetadataDTO;
import in.dhruvpathak.cloudshareapi.repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FileMetadataService {
    private final ProfileService profileService;
    private final FileMetadataRepository fileMetadataRepository;
    private final Cloudinary cloudinary; // Mark as final for @RequiredArgsConstructor to work

    public List<FileMetadataDTO> uploadFiles(MultipartFile[] files) throws IOException {
        List<FileMetadataDTO> result = new ArrayList<>();
        ProfileDocument currentProfile = this.profileService.getCurrentProfile();

        for (MultipartFile file : files) {
            // Uploading to Cloudinary
            // Inside your for-loop in uploadFiles:
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap(
                            "resource_type", "auto",
                            "folder", "cloudshare"
                    ));

            String fileUrl = uploadResult.get("secure_url").toString();
            String publicId = uploadResult.get("public_id").toString();

            // 🔥 Get the assigned resource type from Cloudinary
            String resourceType = uploadResult.get("resource_type").toString();

            FileMetadataDocument doc = FileMetadataDocument.builder()
                    .name(file.getOriginalFilename())
                    .type(file.getContentType())
                    .size(file.getSize())
                    .isPublic(false)
                    .fileLocation(fileUrl)
                    .cloudinaryPublicId(publicId)
                    .resourceType(resourceType) // 🔥 Save it to the DB
                    .clerkId(currentProfile.getClerkId())
                    .uploadedAt(LocalDateTime.now())
                    .build();

            fileMetadataRepository.save(doc);
            result.add(mapToDTO(doc));
        }
        return result;
    }

    public List<FileMetadataDTO> getFiles() {
        ProfileDocument currentProfile = this.profileService.getCurrentProfile();
        return this.fileMetadataRepository.findByClerkId(currentProfile.getClerkId())
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public FileMetadataDTO getPublicFile(String id) {

        FileMetadataDocument doc = fileMetadataRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File not found"));

        if (!doc.getIsPublic()) {
            throw new RuntimeException("This file is not public");
        }

        return mapToDTO(doc);
    }

    public FileMetadataDTO getDownloadableFile(String id) {
        FileMetadataDocument file = fileMetadataRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File not found"));

        String downloadUrl = file.getFileLocation();

        // 🔥 Only add fl_attachment if it's an image or video.
        // Cloudinary throws an error if you apply it to a "raw" file.
        if (!"raw".equals(file.getResourceType())) {
            downloadUrl = downloadUrl.replace("/upload/", "/upload/fl_attachment/");
        }

        FileMetadataDTO dto = mapToDTO(file);
        dto.setFileLocation(downloadUrl);

        return dto;
    }

    public void deleteFile(String id) throws IOException {
        FileMetadataDocument file = fileMetadataRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File not found"));

        // 🔥 Use the exact resource type saved during upload
        String resType = file.getResourceType() != null ? file.getResourceType() : "image"; // Fallback for old files

        // Delete from Cloudinary
        cloudinary.uploader().destroy(file.getCloudinaryPublicId(), ObjectUtils.asMap("resource_type", resType));

        // Delete from DB
        fileMetadataRepository.delete(file);
    }

    public FileMetadataDTO togglePublic(String id) {
        FileMetadataDocument file = fileMetadataRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File not found"));

        // SECURITY FIX: Only the owner can toggle visibility
        ProfileDocument currentProfile = this.profileService.getCurrentProfile();
        if (!file.getClerkId().equals(currentProfile.getClerkId())) {
            throw new RuntimeException("Unauthorized to modify this file");
        }

        file.setIsPublic(!file.getIsPublic());
        fileMetadataRepository.save(file);
        return mapToDTO(file);
    }

    private FileMetadataDTO mapToDTO(FileMetadataDocument doc) {
        return FileMetadataDTO.builder()
                .id(doc.getId())
                .fileLocation(doc.getFileLocation())
                .name(doc.getName())
                .size(doc.getSize())
                .type(doc.getType())
                .clerkId(doc.getClerkId())
                .isPublic(doc.getIsPublic())
                .uploadedAt(doc.getUploadedAt())
                // 🔥 ADDED THESE TWO LINES SO YOUR FRONTEND GETS THE DATA
                .cloudinaryPublicId(doc.getCloudinaryPublicId())
                .resourceType(doc.getResourceType())
                .build();
    }
}