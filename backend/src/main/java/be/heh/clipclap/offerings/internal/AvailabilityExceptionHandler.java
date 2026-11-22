package be.heh.clipclap.offerings.internal;

import be.heh.clipclap.creators.CreatorProfileNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AvailabilityController.class)
class AvailabilityExceptionHandler {

    @ExceptionHandler(SlotNotFoundException.class)
    ResponseEntity<ProblemDetail> handleSlotNotFound(SlotNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Créneau introuvable", ex.getMessage());
    }

    @ExceptionHandler(SlotConflictException.class)
    ResponseEntity<ProblemDetail> handleConflict(SlotConflictException ex) {
        return problem(HttpStatus.CONFLICT, "Créneau en conflit", ex.getMessage());
    }

    @ExceptionHandler(OfferingNotFoundException.class)
    ResponseEntity<ProblemDetail> handleOfferingNotFound(OfferingNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Prestation introuvable", ex.getMessage());
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
        return problem(HttpStatus.BAD_REQUEST, "Disponibilité invalide", ex.getMessage());
    }

    private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String title, String detail) {
        var pd = ProblemDetail.forStatusAndDetail(status, detail);
        if (title != null) {
            pd.setTitle(title);
        }
        return ResponseEntity.status(status).body(pd);
    }
}
