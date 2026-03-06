package in.dhruvpathak.cloudshareapi.service;

import in.dhruvpathak.cloudshareapi.document.UserCredits;
import in.dhruvpathak.cloudshareapi.repository.UserCreditsRepository;
import lombok.Generated;
import org.springframework.stereotype.Service;

@Service
public class UserCreditsService {
    private final UserCreditsRepository userCreditsRepository;
    private final ProfileService profileService;

    public UserCreditsService(UserCreditsRepository userCreditsRepository, ProfileService profileService) {
        this.userCreditsRepository = userCreditsRepository;
        this.profileService = profileService;
    }

    public UserCredits createInitialCredits(String clerkId) {
        UserCredits userCredits = UserCredits.builder().clerkId(clerkId).credits(5).plan("BASIC").build();
        return (UserCredits) this.userCreditsRepository.save(userCredits);
    }

    public UserCredits getUserCredits(String clerkId) {
        return (UserCredits) this.userCreditsRepository. findByClerkId(clerkId).orElseGet(() -> {
            return this.createInitialCredits(clerkId);
        });
    }

    public UserCredits getUserCredits() {
        String clerkId = this.profileService.getCurrentProfile().getClerkId();
        return this.getUserCredits(clerkId);
    }

    public Boolean hasEnoughCredits(int requiredCredits) {
        UserCredits userCredits = this.getUserCredits();
        return userCredits.getCredits() >= requiredCredits;
    }

    public UserCredits consumeCredit() {
        UserCredits userCredits = this.getUserCredits();
        if (userCredits.getCredits() <= 0) {
            return null;
        } else {
            userCredits.setCredits(userCredits.getCredits() - 1);
            return (UserCredits) this.userCreditsRepository.save(userCredits);
        }
    }

    public UserCredits addCredits(String clerkId, Integer creditsToAdd, String plan) {
        UserCredits userCredits = (UserCredits) this.userCreditsRepository.findByClerkId(clerkId).orElseGet(() -> {
            return this.createInitialCredits(clerkId);
        });
        userCredits.setCredits(userCredits.getCredits() + creditsToAdd);
        userCredits.setPlan(plan);
        return (UserCredits) this.userCreditsRepository.save(userCredits);
    }
}
