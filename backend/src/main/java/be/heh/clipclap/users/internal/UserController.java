package be.heh.clipclap.users.internal;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
class UserController {

	private final UserService userService;

	UserController(UserService userService) {
		this.userService = userService;
	}

	/**
	 * Utilisateur connecté. Le compte local est créé à la première connexion, une fois le
	 * rôle choisi.
	 */
	@GetMapping("/me")
	UserAccount me(@AuthenticationPrincipal Jwt jwt) {
		return userService.findOrCreate(jwt);
	}

	/**
	 * Choix du rôle à l'inscription (client ou créateur) : attribué dans Zitadel puis en base.
	 */
	@PostMapping("/me/role")
	UserAccount choisirRole(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AssignRoleRequest request) {
		return userService.choisirRole(jwt, request.role());
	}

	@ExceptionHandler(RoleInvalideException.class)
	ResponseEntity<Void> gererRoleInvalide() {
		return ResponseEntity.badRequest().build();
	}

	@ExceptionHandler(RoleDejaChoisiException.class)
	ResponseEntity<Void> gererRoleDejaChoisi() {
		return ResponseEntity.status(HttpStatus.CONFLICT).build();
	}

}
