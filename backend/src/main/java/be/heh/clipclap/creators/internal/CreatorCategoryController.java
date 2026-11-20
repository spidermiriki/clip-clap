package be.heh.clipclap.creators.internal;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Types de prestation du créateur connecté (#87). La liste des catégories existantes vient de
 * {@code GET /api/catalog/categories} : creators n'appelle pas catalog, qui dépend déjà de lui.
 */
@RestController
@RequestMapping("/api/creators/me/categories")
class CreatorCategoryController {

    record CategorySelection(List<Integer> categoryIds) {}

    private final CreatorCategoryService service;
    private final CurrentUser currentUser;

    CreatorCategoryController(CreatorCategoryService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping
    List<Integer> list(Authentication auth) {
        return service.findMine(currentUser.id(auth));
    }

    @PutMapping
    List<Integer> replace(Authentication auth, @RequestBody(required = false) CategorySelection body) {
        return service.replaceMine(currentUser.id(auth), body == null ? null : body.categoryIds());
    }
}
