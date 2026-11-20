package be.heh.clipclap.offerings.internal;

import be.heh.clipclap.creators.CreatorProfileNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = OfferingController.class)
class OfferingExceptionHandler {

    @ExceptionHandler(OfferingNotFoundException.class)
    ResponseEntity<ProblemDetail> handleNotFound(OfferingNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Prestation introuvable", ex.getMessage());
    }

    @ExceptionHandler(OfferingInUseException.class)
    ResponseEntity<ProblemDetail> handleInUse(OfferingInUseException ex) {
        return problem(HttpStatus.CONFLICT, "Prestation réservée", ex.getMessage());
    }

    @ExceptionHandler(UnknownCategoryException.class)
    ResponseEntity<ProblemDetail> handleUnknownCategory(UnknownCategoryException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Catégorie inconnue", ex.getMessage());
    }

    @ExceptionHandler(CreatorProfileNotFoundException.class)
    ResponseEntity<ProblemDetail> handleProfileNotFound(CreatorProfileNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Profil introuvable", ex.getMessage());
    }

    @ExceptionHandler(UserNotFoundException.class)
    ResponseEntity<ProblemDetail> handleUserNotFound(UserNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Utilisateur inconnu", ex.getMessage());
    }

    @ExceptionHandler(UnauthenticatedException.class)
    ResponseEntity<ProblemDetail> handleUnauthenticated(UnauthenticatedException ex) {
        return problem(HttpStatus.UNAUTHORIZED, null, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ProblemDetail> handleIllegalArgument(IllegalArgumentException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Prestation invalide", ex.getMessage());
    }

    private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String title, String detail) {
        var pd = ProblemDetail.forStatusAndDetail(status, detail);
        if (title != null) {
            pd.setTitle(title);
        }
        return ResponseEntity.status(status).body(pd);
    }
}
