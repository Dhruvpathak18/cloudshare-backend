package in.dhruvpathak.cloudshareapi.controller;

import in.dhruvpathak.cloudshareapi.dto.ProfileDTO;
import in.dhruvpathak.cloudshareapi.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor

public class ProfileController {
    private final ProfileService profileService;

    @PostMapping("/register")
    public ResponseEntity<?> registerProfile(@RequestBody ProfileDTO profileDTO) {
        // 1. Check if the user already exists to avoid MongoDB Duplicate Key errors
        if (profileService.existsByClerkId(profileDTO.getClerkId())) {
            return ResponseEntity.ok("User already exists, skipping creation.");
        }

        // 2. If they don't exist, create the profile
        ProfileDTO savedProfile = profileService.createProfile(profileDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedProfile);
    }
}
