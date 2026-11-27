package be.heh.clipclap.bookings.internal;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = BookingController.class)
class BookingExceptionHandler {

    @ExceptionHandler(BookingNotFoundException.class)
    ResponseEntity<ProblemDetail> handleNotFound(BookingNotFoundException ex) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Réservation introuvable");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
    }

    @ExceptionHandler(BookingAccessDeniedException.class)
    ResponseEntity<ProblemDetail> handleAccessDenied(BookingAccessDeniedException ex) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        pd.setTitle("Accès refusé");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(pd);
    }

    @ExceptionHandler(InvalidBookingTransitionException.class)
    ResponseEntity<ProblemDetail> handleInvalidTransition(InvalidBookingTransitionException ex) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Transition invalide");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(pd);
    }

    @ExceptionHandler(UnauthenticatedException.class)
    ResponseEntity<ProblemDetail> handleUnauthenticated(UnauthenticatedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage()));
    }

    @ExceptionHandler(UserNotFoundException.class)
    ResponseEntity<ProblemDetail> handleUserNotFound(UserNotFoundException ex) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Utilisateur inconnu");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
    }

    @ExceptionHandler(OfferingNotFoundException.class)
    ResponseEntity<ProblemDetail> handleOfferingNotFound(OfferingNotFoundException ex) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Prestation introuvable");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
    }

    @ExceptionHandler(SlotNotFoundException.class)
    ResponseEntity<ProblemDetail> handleSlotNotFound(SlotNotFoundException ex) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Créneau introuvable");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
    }

    @ExceptionHandler(SlotNotBookableException.class)
    ResponseEntity<ProblemDetail> handleSlotNotBookable(SlotNotBookableException ex) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Créneau non réservable");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(pd);
    }

    @ExceptionHandler(SelfBookingException.class)
    ResponseEntity<ProblemDetail> handleSelfBooking(SelfBookingException ex) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        pd.setTitle("Action refusée");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(pd);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ProblemDetail> handleIllegalArgument(IllegalArgumentException ex) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Réservation invalide");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
    }
}
