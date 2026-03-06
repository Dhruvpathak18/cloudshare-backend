package in.dhruvpathak.cloudshareapi.controller;

import in.dhruvpathak.cloudshareapi.document.UserCredits;
import in.dhruvpathak.cloudshareapi.dto.FileMetadataDTO;
import in.dhruvpathak.cloudshareapi.service.FileMetadataService;
import in.dhruvpathak.cloudshareapi.service.UserCreditsService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/files"})
@RequiredArgsConstructor
public class FileController {
    private final FileMetadataService fileMetadataService;
    private final UserCreditsService userCreditsService;

    @PostMapping({"/upload"})
    public ResponseEntity<?> uploadFiles(@RequestPart("files") MultipartFile[] files) throws IOException {
        Map<String, Object> response = new HashMap();
        List<FileMetadataDTO> list = this.fileMetadataService.uploadFiles(files);
        UserCredits finalCredits = this.userCreditsService.getUserCredits();
        response.put("files", list);
        response.put("remainingCredits", finalCredits.getCredits());
        return ResponseEntity.ok(response);
    }

    @GetMapping({"/my"})
    public ResponseEntity<?> getFilesForCurrentUser() {
        List<FileMetadataDTO> files = this.fileMetadataService.getFiles();
        return ResponseEntity.ok(files);
    }

    @GetMapping({"/public/{id}"})
    public ResponseEntity<?> getPublicFile(@PathVariable String id) {
        FileMetadataDTO file = this.fileMetadataService.getPublicFile(id);
        return ResponseEntity.ok(file);
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Void> download(@PathVariable String id) {

        FileMetadataDTO file = fileMetadataService.getDownloadableFile(id);

        return ResponseEntity.status(302)
                .header("Location", file.getFileLocation())
                .build();
    }

    @DeleteMapping({"/{id}"})
    public ResponseEntity<?> deleteFile(@PathVariable String id) throws IOException {
        this.fileMetadataService.deleteFile(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping({"/{id}/toggle-public"})
    public ResponseEntity<?> togglePublic(@PathVariable String id) {
        FileMetadataDTO file = this.fileMetadataService.togglePublic(id);
        return ResponseEntity.ok(file);
    }}
