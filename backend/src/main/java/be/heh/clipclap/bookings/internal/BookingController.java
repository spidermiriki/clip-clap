package be.heh.clipclap.bookings.internal;

import be.heh.clipclap.bookings.Booking;
import be.heh.clipclap.bookings.BookingDetail;
import be.heh.clipclap.bookings.BookingView;
import be.heh.clipclap.bookings.CreatorBookings;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/api/bookings")
class BookingController {

    private final BookingServiceImpl bookingService;
    private final CurrentUser currentUser;

    BookingController(BookingServiceImpl bookingService, CurrentUser currentUser) {
        this.bookingService = bookingService;
        this.currentUser = currentUser;
    }

    @PostMapping
    @ResponseStatus(CREATED)
    Booking creer(Authentication auth, @RequestBody BookingCommand command) {
        return bookingService.creer(currentUser.id(auth), command);
    }

    @GetMapping("/{bookingId}")
    BookingDetail detail(Authentication auth, @PathVariable long bookingId) {
        return bookingService.detail(bookingId, currentUser.id(auth));
    }

    /** Réservations reçues par le créateur connecté (#31), tableau de bord #32. */
    @GetMapping("/creator")
    CreatorBookings mine(Authentication auth) {
        return bookingService.listerPourCreateur(currentUser.id(auth));
    }

    @PostMapping("/{bookingId}/accept")
    BookingView accept(Authentication auth, @PathVariable long bookingId) {
        return bookingService.accepter(bookingId, currentUser.id(auth));
    }

    @PostMapping("/{bookingId}/refuse")
    BookingView refuse(Authentication auth, @PathVariable long bookingId,
                       @RequestBody(required = false) RefuseCommand command) {
        return bookingService.refuser(bookingId, currentUser.id(auth), command);
    }
}
