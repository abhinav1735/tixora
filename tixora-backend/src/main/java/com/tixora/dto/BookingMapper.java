
package com.tixora.dto;

import com.tixora.entity.Booking;
import com.tixora.entity.BookingSeat;

import java.util.List;

public class BookingMapper {

    private BookingMapper() {
        // Utility class
    }

    public static BookingResponse toResponse(
            Booking booking,
            List<BookingSeat> bookingSeats
    ) {

        if (booking == null || booking.getShow() == null) {
            throw new IllegalArgumentException(
                    "Booking and its show are required."
            );
        }

        var show = booking.getShow();

        if (show.getMovie() == null
                || show.getScreen() == null
                || show.getScreen().getTheatre() == null) {
            throw new IllegalStateException(
                    "Booking must have complete show details."
            );
        }

        List<String> seatLabels = bookingSeats.stream()
                .map(BookingSeat::getSeat)
                .map(seat -> seat.getRowLabel() + seat.getSeatNumber())
                .toList();

        return new BookingResponse(
                booking.getId(),
                booking.getBookingReference(),

                show.getId(),
                show.getMovie().getTitle(),
                show.getScreen().getTheatre().getName(),
                show.getScreen().getName(),

                booking.getCustomerName(),
                booking.getCustomerEmail(),

                seatLabels,
                booking.getTotalAmount(),
                booking.getStatus(),
                booking.getCreatedAt()
        );
    }
}
