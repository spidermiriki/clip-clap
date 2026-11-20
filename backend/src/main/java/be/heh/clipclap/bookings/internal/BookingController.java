package be.heh.clipclap.bookings.internal;

import be.heh.clipclap.bookings.BookingDetail;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
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

    @GetMapping("/{bookingId}")
    BookingDetail detail(Authentication auth, @PathVariable long bookingId) {
        return bookingService.detail(bookingId, currentUser.id(auth));
    }
}
