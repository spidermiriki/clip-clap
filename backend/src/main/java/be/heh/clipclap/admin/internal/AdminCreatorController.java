package be.heh.clipclap.admin.internal;

import be.heh.clipclap.creators.CreatorProfile;
import be.heh.clipclap.creators.CreatorProfileService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Validation des comptes créateurs par l'administrateur (#33, wireframe 11). Le rôle ADMIN est
 * exigé pour tout {@code /api/admin/**} par la configuration de sécurité du module users.
 */
@RestController
@RequestMapping("/api/admin/creators")
class AdminCreatorController {

    record Rejection(String reason) {}

    private final CreatorProfileService creatorProfileService;

    AdminCreatorController(CreatorProfileService creatorProfileService) {
        this.creatorProfileService = creatorProfileService;
    }

    @GetMapping
    List<CreatorProfile> list(@RequestParam(defaultValue = "PENDING") String status) {
        return creatorProfileService.findByValidationStatus(status);
    }

    @PostMapping("/{userId}/validate")
    CreatorProfile validate(@PathVariable long userId) {
        return creatorProfileService.validate(userId);
    }

    @PostMapping("/{userId}/reject")
    CreatorProfile reject(@PathVariable long userId, @RequestBody(required = false) Rejection body) {
        return creatorProfileService.reject(userId, body == null ? null : body.reason());
    }
}
