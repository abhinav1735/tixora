package com.tixora.dto;

import com.tixora.entity.Booking;
import com.tixora.entity.BookingSeat;
import com.tixora.entity.PaymentStatus;

import java.util.List;

public final class BookingMapper {

    private BookingMapper() {
    }

    public static BookingResponse toResponse(
            Booking booking,
            List<BookingSeat> bookingSeats
    ) {
        return toResponse(booking, bookingSeats, null);
    }

    public static BookingResponse toResponse(
            Booking booking,
            List<BookingSeat> bookingSeats,
            PaymentStatus paymentStatus
    ) {
        if (booking == null || booking.getShow() == null) {
            throw new IllegalArgumentException("Booking and show are required.");
        }

        var show = booking.getShow();

        if (show.getMovie() == null
                || show.getScreen() == null
                || show.getScreen().getTheatre() == null) {
            throw new IllegalStateException("Booking details are incomplete.");
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
                booking.getTicketSubtotal(),
                booking.getConvenienceFeeAmount(),
                booking.getGstOnTicketAmount(),
                booking.getGstOnConvenienceFeeAmount(),
                booking.getGstAmount(),
                booking.getTotalAmount(),
                booking.getFeeTypeSnapshot() == null
                        ? null : booking.getFeeTypeSnapshot().name(),
                booking.getConvenienceFeeUnitSnapshot(),
                booking.getGstEnabledSnapshot(),
                booking.getGstPercentageSnapshot(),
                booking.getGstOnTicketPriceSnapshot(),
                booking.getGstOnConvenienceFeeSnapshot(),
                booking.getCancellationThresholdHoursSnapshot(),
                booking.getCancellationPercentageBeforeThresholdSnapshot(),
                booking.getCancellationPercentageWithinThresholdSnapshot(),
                booking.getCancellationAllowedAfterShowStartsSnapshot(),
                booking.getStatus(),
                paymentStatus,
                booking.getCreatedAt()
        );
    }
}
