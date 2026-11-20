package be.heh.clipclap.bookings.internal;

import be.heh.clipclap.users.UserApi;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/** Identifiant local de l'utilisateur connecté, retrouvé à partir du sub de son token. */
// Nom de bean explicite : un CurrentUser homonyme existe déjà dans creators.internal,
// et le scan par défaut nommerait les deux "currentUser" (ConflictingBeanDefinitionException
// au démarrage complet de l'application, pas seulement dans les tests multi-modules)
@Component("bookingsCurrentUser")
class CurrentUser {

    private final UserApi userApi;

    CurrentUser(UserApi userApi) {
        this.userApi = userApi;
    }

    long id(Authentication auth) {
        if (auth == null || auth instanceof AnonymousAuthenticationToken) {
            throw new UnauthenticatedException();
        }
        return userApi.findIdBySub(auth.getName())
                .orElseThrow(UserNotFoundException::new);
    }
}
