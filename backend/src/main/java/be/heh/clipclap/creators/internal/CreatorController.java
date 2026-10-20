package be.heh.clipclap.creators.internal;

import be.heh.clipclap.creators.CreateProfileCommand;
import be.heh.clipclap.creators.CreatorProfile;
import be.heh.clipclap.creators.CreatorProfileNotFoundException;
import be.heh.clipclap.creators.CreatorProfileService;
import be.heh.clipclap.users.UserApi;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/creators")
class CreatorController {

    private final CreatorProfileService creatorProfileService;
    private final UserApi userApi;

    CreatorController(CreatorProfileService creatorProfileService, UserApi userApi) {
        this.creatorProfileService = creatorProfileService;
        this.userApi = userApi;
    }

    @PostMapping("/me")
    @ResponseStatus(HttpStatus.CREATED)
    CreatorProfile create(Authentication auth, @RequestBody CreateProfileCommand command) {
        long userId = resolveUserId(auth);
        return creatorProfileService.create(userId, command);
    }

    @PutMapping("/me")
    CreatorProfile update(Authentication auth, @RequestBody CreateProfileCommand command) {
        long userId = resolveUserId(auth);
        return creatorProfileService.update(userId, command);
    }

    @GetMapping("/me")
    CreatorProfile getProfile(Authentication auth) {
        long userId = resolveUserId(auth);
        return creatorProfileService.findByUserId(userId)
                .orElseThrow(() -> new CreatorProfileNotFoundException(userId));
    }

    private long resolveUserId(Authentication auth) {
        if (auth == null || auth instanceof AnonymousAuthenticationToken) {
            throw new UnauthenticatedException();
        }
        return userApi.findIdBySub(auth.getName())
                .orElseThrow(UserNotFoundException::new);
    }
}
