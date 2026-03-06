package in.dhruvpathak.cloudshareapi.controller;
import in.dhruvpathak.cloudshareapi.document.UserCredits;
import in.dhruvpathak.cloudshareapi.dto.UserCreditsDTO;
import in.dhruvpathak.cloudshareapi.service.UserCreditsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserCreditsController {

    private final UserCreditsService userCreditsService;

    @GetMapping("/credits")
    public ResponseEntity<?>getUsersCredits(){
        UserCredits userCredits = userCreditsService.getUserCredits();
        UserCreditsDTO response = UserCreditsDTO.builder().credits(userCredits.getCredits()).plan(userCredits.getPlan()).build();
        return ResponseEntity.ok(response);
    }
    }

