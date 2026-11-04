
package com.tixora.service;

import com.tixora.dto.BookingMapper;
import com.tixora.dto.BookingRequest;
import com.tixora.dto.BookingResponse;
import com.tixora.dto.SeatAvailabilityResponse;

import com.tixora.entity.Booking;
import com.tixora.entity.BookingSeat;
import com.tixora.entity.BookingStatus;
import com.tixora.entity.Seat;
import com.tixora.entity.SeatReservation;
import com.tixora.entity.Show;

import com.tixora.exception.ResourceNotFoundException;

import com.tixora.repository.BookingRepository;
import com.tixora.repository.BookingSeatRepository;
import com.tixora.repository.SeatRepository;
import com.tixora.repository.SeatReservationRepository;
import com.tixora.repository.ShowRepository;
import com.tixora.repository.PaymentRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class BookingService {

    private static final long BOOKING_EXPIRATION_MINUTES = 10;

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final SeatRepository seatRepository;
    private final SeatReservationRepository seatReservationRepository;
    private final ShowRepository showRepository;
    private final BookingExpirationService bookingExpirationService;
    private final PaymentRepository paymentRepository;

    public BookingService(
            BookingRepository bookingRepository,
            BookingSeatRepository bookingSeatRepository,
            SeatRepository seatRepository,
            SeatReservationRepository seatReservationRepository,
            ShowRepository showRepository,
            BookingExpirationService bookingExpirationService,
            PaymentRepository paymentRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.seatRepository = seatRepository;
        this.seatReservationRepository = seatReservationRepository;
        this.showRepository = showRepository;
        this.bookingExpirationService = bookingExpirationService;
        this.paymentRepository = paymentRepository;
    }

    // Get seat availability for a particular show
    @Transactional
    public List<SeatAvailabilityResponse> getSeatAvailability(Long showId) {

        // Release expired reservations before checking availability.
        bookingExpirationService.expirePendingBookings();

        Show show = getValidShow(showId);

        List<Seat> seats = seatRepository
                .findByScreenIdOrderByRowLabelAscSeatNumberAsc(
                        show.getScreen().getId()
                );

        Set<Long> reservedSeatIds = new HashSet<>(
                seatReservationRepository.findReservedSeatIdsByShowId(showId)
        );

        return seats.stream()
                .filter(Seat::isActive)
                .map(seat -> new SeatAvailabilityResponse(
                        seat.getId(),
                        seat.getRowLabel(),
                        seat.getSeatNumber(),
                        seat.getRowLabel() + seat.getSeatNumber(),
                        seat.getCategory(),
                        !reservedSeatIds.contains(seat.getId())
                ))
                .toList();
    }

    // Create a booking
    @Transactional
    public BookingResponse createBooking(BookingRequest request) {

        // Expire old bookings before attempting a new reservation.
        bookingExpirationService.expirePendingBookings();

        Show show = getValidShow(request.getShowId());

        List<Long> requestedSeatIds = request.getSeatIds();

        // Prevent duplicate seat IDs in the same request.
        Set<Long> uniqueSeatIds = new HashSet<>(requestedSeatIds);

        if (uniqueSeatIds.size() != requestedSeatIds.size()) {
            throw new IllegalArgumentException(
                    "Duplicate seat IDs are not allowed."
            );
        }

        // Lock selected seats in the database.
        // This serializes concurrent booking attempts for the same seats.
        List<Seat> selectedSeats = seatRepository
                .findAllByIdInForUpdate(requestedSeatIds);

        if (selectedSeats.size() != requestedSeatIds.size()) {
            throw new ResourceNotFoundException(
                    "One or more selected seats do not exist."
            );
        }

        // Validate selected seats.
        for (Seat seat : selectedSeats) {

            if (!seat.isActive()) {
                throw new IllegalArgumentException(
                        "Seat " + seat.getRowLabel()
                                + seat.getSeatNumber()
                                + " is inactive."
                );
            }

            if (!seat.getScreen().getId()
                    .equals(show.getScreen().getId())) {

                throw new IllegalArgumentException(
                        "Selected seat does not belong to the show's screen."
                );
            }
        }

        // Check existing reservations after acquiring seat locks.
        Set<Long> reservedSeatIds = new HashSet<>(
                seatReservationRepository.findReservedSeatIdsByShowId(
                        show.getId()
                )
        );

        for (Seat seat : selectedSeats) {

            if (reservedSeatIds.contains(seat.getId())) {
                throw new IllegalStateException(
                        "Seat " + seat.getRowLabel()
                                + seat.getSeatNumber()
                                + " is already reserved."
                );
            }
        }

        // Calculate total amount on the backend.
        BigDecimal totalAmount = show.getTicketPrice()
                .multiply(BigDecimal.valueOf(selectedSeats.size()));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt =
                now.plusMinutes(BOOKING_EXPIRATION_MINUTES);

        // Create pending booking.
        Booking booking = new Booking();

        booking.setShow(show);
        booking.setCustomerName(request.getCustomerName().trim());
        booking.setCustomerEmail(
                request.getCustomerEmail().trim().toLowerCase()
        );
        booking.setTotalAmount(totalAmount);
        booking.setStatus(BookingStatus.PENDING);
        booking.setExpiresAt(expiresAt);

        Booking savedBooking = bookingRepository.save(booking);

        // Save permanent booking-seat history.
        List<BookingSeat> bookingSeats = selectedSeats.stream()
                .map(seat -> {

                    BookingSeat bookingSeat = new BookingSeat();

                    bookingSeat.setBooking(savedBooking);
                    bookingSeat.setShow(show);
                    bookingSeat.setSeat(seat);
                    bookingSeat.setTicketPrice(show.getTicketPrice());

                    return bookingSeat;
                })
                .toList();

        List<BookingSeat> savedBookingSeats =
                bookingSeatRepository.saveAll(bookingSeats);

        // Create temporary seat reservations.
        List<SeatReservation> reservations = selectedSeats.stream()
                .map(seat -> {

                    SeatReservation reservation = new SeatReservation();

                    reservation.setBooking(savedBooking);
                    reservation.setShow(show);
                    reservation.setSeat(seat);
                    reservation.setExpiresAt(expiresAt);

                    return reservation;
                })
                .toList();

        // Unique database constraint provides an additional
        // safeguard against duplicate reservations.
        seatReservationRepository.saveAllAndFlush(reservations);

        return BookingMapper.toResponse(
                savedBooking,
                savedBookingSeats
        );
    }

    private Show getValidShow(Long showId) {

        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Show with ID " + showId + " does not exist."
                ));

        if (!show.isActive()) {
            throw new IllegalStateException(
                    "This show is currently inactive."
            );
        }

        if (!show.getStartAt().isAfter(LocalDateTime.now())) {
            throw new IllegalStateException(
                    "Booking is not available because this show has already started."
            );
        }

        if (!show.getScreen().isActive()) {
            throw new IllegalStateException(
                    "The screen for this show is currently inactive."
            );
        }

        return show;
    }

    @Transactional(readOnly = true)
    public BookingResponse getBookingDetails(String bookingReference) {

        if (bookingReference == null || bookingReference.isBlank()) {
            throw new IllegalArgumentException(
                    "Booking reference is required."
            );
        }

        Booking booking = bookingRepository
                .findWithDetailsByBookingReference(bookingReference.trim())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking not found."
                ));

        List<BookingSeat> bookingSeats =
                bookingSeatRepository.findByBookingId(booking.getId());

        var latestPayment = paymentRepository
                .findFirstByBookingIdOrderByCreatedAtDesc(booking.getId());

        var paymentStatus = latestPayment
                .map(payment -> payment.getStatus())
                .orElse(null);

        return BookingMapper.toResponse(
                booking,
                bookingSeats,
                paymentStatus
        );
    }
}
