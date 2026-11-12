package be.heh.clipclap.creators.internal;

import be.heh.clipclap.users.UserApi;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/** Identifiant local de l'utilisateur connecté, retrouvé à partir du sub de son token. */
@Component
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
