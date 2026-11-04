
package com.tixora.dto;

import com.tixora.entity.Booking;
import com.tixora.entity.BookingSeat;
import com.tixora.entity.PaymentStatus;

import java.util.List;

public class BookingMapper {

    private BookingMapper() {
    }

    // Used by the existing booking creation flow.
    public static BookingResponse toResponse(
            Booking booking,
            List<BookingSeat> bookingSeats
    ) {
        return toResponse(booking, bookingSeats, null);
    }

    // Used when fetching complete booking details.
    public static BookingResponse toResponse(
            Booking booking,
            List<BookingSeat> bookingSeats,
            PaymentStatus paymentStatus
    ) {
        if (booking == null || booking.getShow() == null) {
            throw new IllegalArgumentException("Booking and show are required");
        }

        var show = booking.getShow();

        if (show.getMovie() == null
                || show.getScreen() == null
                || show.getScreen().getTheatre() == null) {
            throw new IllegalStateException("Booking details are incomplete");
        }

        List<String> seatLabels = bookingSeats.stream()
                .map(BookingSeat::getSeat)
                .map(seat -> seat.getRowLabel() + seat.getSeatNumber())
                .sorted()
                .toList();

        return new BookingResponse(
                booking.getId(),
                booking.getBookingReference(),
                show.getId(),
                show.getMovie().getTitle(),
                show.getScreen().getTheatre().getName(),
                show.getScreen().getName(),
                show.getStartAt(),
                show.getEndAt(),
                show.getTicketPrice(),
                booking.getCustomerName(),
                booking.getCustomerEmail(),
                seatLabels,
                booking.getTotalAmount(),
                booking.getStatus(),
                paymentStatus,
                booking.getCreatedAt()
        );
    }
}
