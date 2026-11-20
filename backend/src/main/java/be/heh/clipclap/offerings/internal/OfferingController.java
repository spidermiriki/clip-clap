package be.heh.clipclap.offerings.internal;

import be.heh.clipclap.offerings.Offering;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Prestations à prix fixe du créateur connecté (#27), avec leurs options payantes. */
@RestController
@RequestMapping("/api/offerings/me")
class OfferingController {

    private final OfferingService offeringService;
    private final CurrentUser currentUser;

    OfferingController(OfferingService offeringService, CurrentUser currentUser) {
        this.offeringService = offeringService;
        this.currentUser = currentUser;
    }

    @GetMapping
    List<Offering> list(Authentication auth) {
        return offeringService.listMine(currentUser.id(auth));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Offering create(Authentication auth, @RequestBody OfferingCommand command) {
        return offeringService.create(currentUser.id(auth), command);
    }

    @PutMapping("/{offeringId}")
    Offering update(Authentication auth, @PathVariable long offeringId, @RequestBody OfferingCommand command) {
        return offeringService.update(currentUser.id(auth), offeringId, command);
    }

    @DeleteMapping("/{offeringId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(Authentication auth, @PathVariable long offeringId) {
        offeringService.delete(currentUser.id(auth), offeringId);
    }
}
