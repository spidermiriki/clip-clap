package be.heh.clipclap.admin.internal;

import be.heh.clipclap.creators.CreatorProfileNotFoundException;
import be.heh.clipclap.creators.CreatorProfileNotPendingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AdminCreatorController.class)
class AdminExceptionHandler {

    @ExceptionHandler(CreatorProfileNotFoundException.class)
    ResponseEntity<ProblemDetail> handleNotFound(CreatorProfileNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Profil introuvable", ex.getMessage());
    }

    @ExceptionHandler(CreatorProfileNotPendingException.class)
    ResponseEntity<ProblemDetail> handleNotPending(CreatorProfileNotPendingException ex) {
        return problem(HttpStatus.CONFLICT, "Profil déjà traité", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ProblemDetail> handleIllegalArgument(IllegalArgumentException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Requête invalide", ex.getMessage());
    }

    private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String title, String detail) {
        var pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        return ResponseEntity.status(status).body(pd);
    }
}
