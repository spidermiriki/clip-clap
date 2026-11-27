package be.heh.clipclap.bookings.internal;

import be.heh.clipclap.bookings.Booking;
import be.heh.clipclap.bookings.BookingDetail;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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
    @ResponseStatus(HttpStatus.CREATED)
    Booking creer(Authentication auth, @RequestBody BookingCommand command) {
        return bookingService.creer(currentUser.id(auth), command);
    }

    @GetMapping("/{bookingId}")
    BookingDetail detail(Authentication auth, @PathVariable long bookingId) {
        return bookingService.detail(bookingId, currentUser.id(auth));
    }
}
