package be.heh.clipclap.users.internal;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
class UserController {

	private final UserService userService;

	UserController(UserService userService) {
		this.userService = userService;
	}

	/**
	 * Utilisateur connecté. Le compte local est créé à la première connexion.
	 */
	@GetMapping("/me")
	UserAccount me(@AuthenticationPrincipal Jwt jwt) {
		return userService.findOrCreate(jwt.getSubject(), jwt.getTokenValue());
	}

}
