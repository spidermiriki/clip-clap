package be.heh.clipclap.creators.internal;

import be.heh.clipclap.creators.CreateProfileCommand;
import be.heh.clipclap.creators.CreatorProfile;
import be.heh.clipclap.creators.CreatorProfileNotFoundException;
import be.heh.clipclap.creators.CreatorProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/creators")
class CreatorController {

    private final CreatorProfileService creatorProfileService;
    private final CurrentUser currentUser;

    CreatorController(CreatorProfileService creatorProfileService, CurrentUser currentUser) {
        this.creatorProfileService = creatorProfileService;
        this.currentUser = currentUser;
    }

    @PostMapping("/me")
    @ResponseStatus(HttpStatus.CREATED)
    CreatorProfile create(Authentication auth, @RequestBody CreateProfileCommand command) {
        return creatorProfileService.create(currentUser.id(auth), command);
    }

    @PutMapping("/me")
    CreatorProfile update(Authentication auth, @RequestBody CreateProfileCommand command) {
        return creatorProfileService.update(currentUser.id(auth), command);
    }

    @GetMapping("/me")
    CreatorProfile getProfile(Authentication auth) {
        long userId = currentUser.id(auth);
        return creatorProfileService.findByUserId(userId)
                .orElseThrow(() -> new CreatorProfileNotFoundException(userId));
    }
}
