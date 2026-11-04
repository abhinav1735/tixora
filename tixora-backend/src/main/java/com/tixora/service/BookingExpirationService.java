
package com.tixora.service;

import com.tixora.entity.Booking;
import com.tixora.entity.BookingStatus;
import com.tixora.repository.BookingRepository;
import com.tixora.repository.SeatReservationRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingExpirationService {

    private final BookingRepository bookingRepository;
    private final SeatReservationRepository seatReservationRepository;

    public BookingExpirationService(
            BookingRepository bookingRepository,
            SeatReservationRepository seatReservationRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.seatReservationRepository = seatReservationRepository;
    }

    @Scheduled(fixedDelayString = "${tixora.booking.expiration-check-ms:30000}")
    @Transactional
    public void expirePendingBookings() {

        LocalDateTime now = LocalDateTime.now();

        List<Booking> expiredBookings =
                bookingRepository.findByStatusAndExpiresAtLessThanEqual(
                        BookingStatus.PENDING,
                        now
                );

        if (expiredBookings.isEmpty()) {
            return;
        }

        List<Long> expiredBookingIds = expiredBookings.stream()
                .map(Booking::getId)
                .toList();

        // Mark expired bookings.
        expiredBookings.forEach(booking ->
                booking.setStatus(BookingStatus.EXPIRED)
        );

        bookingRepository.saveAll(expiredBookings);

        // Release their reserved seats.
        seatReservationRepository.deleteByBookingIdIn(expiredBookingIds);
    }
}
