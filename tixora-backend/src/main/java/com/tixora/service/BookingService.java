package com.tixora.service;

import com.tixora.dto.BookingMapper;
import com.tixora.dto.BookingRequest;
import com.tixora.dto.BookingResponse;
import com.tixora.dto.SeatAvailabilityResponse;
import com.tixora.entity.Booking;
import com.tixora.entity.BookingSeat;
import com.tixora.entity.BookingStatus;
import com.tixora.entity.PricingSettings;
import com.tixora.entity.Seat;
import com.tixora.entity.SeatReservation;
import com.tixora.entity.Show;
import com.tixora.entity.User;
import com.tixora.exception.ResourceNotFoundException;
import com.tixora.repository.BookingRepository;
import com.tixora.repository.BookingSeatRepository;
import com.tixora.repository.PaymentRepository;
import com.tixora.repository.PricingSettingsRepository;
import com.tixora.repository.SeatRepository;
import com.tixora.repository.SeatReservationRepository;
import com.tixora.repository.ShowRepository;
import com.tixora.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class BookingService {

    private static final long BOOKING_EXPIRATION_MINUTES = 10;
    private static final int MONEY_SCALE = 2;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final BigDecimal ZERO_MONEY = new BigDecimal("0.00");

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final SeatRepository seatRepository;
    private final SeatReservationRepository seatReservationRepository;
    private final ShowRepository showRepository;
    private final BookingExpirationService bookingExpirationService;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final PricingSettingsRepository pricingSettingsRepository;

    public BookingService(
            BookingRepository bookingRepository,
            BookingSeatRepository bookingSeatRepository,
            SeatRepository seatRepository,
            SeatReservationRepository seatReservationRepository,
            ShowRepository showRepository,
            BookingExpirationService bookingExpirationService,
            PaymentRepository paymentRepository,
            UserRepository userRepository,
            PricingSettingsRepository pricingSettingsRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.seatRepository = seatRepository;
        this.seatReservationRepository = seatReservationRepository;
        this.showRepository = showRepository;
        this.bookingExpirationService = bookingExpirationService;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.pricingSettingsRepository = pricingSettingsRepository;
    }

    @Transactional
    public List<SeatAvailabilityResponse> getSeatAvailability(Long showId) {
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

    @Transactional
    public BookingResponse createBooking(BookingRequest request) {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null
                || authentication.getName().equals("anonymousUser")) {
            throw new AccessDeniedException(
                    "Authentication is required to create a booking."
            );
        }

        String authenticatedEmail = authentication.getName()
                .trim()
                .toLowerCase(Locale.ROOT);

        User customer = userRepository.findByEmail(authenticatedEmail)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Authenticated customer account was not found."
                ));

        if (!customer.isEnabled()) {
            throw new AccessDeniedException("Your account is disabled.");
        }

        bookingExpirationService.expirePendingBookings();

        Show show = getValidShow(request.getShowId());
        List<Long> requestedSeatIds = request.getSeatIds();

        Set<Long> uniqueSeatIds = new HashSet<>(requestedSeatIds);
        if (uniqueSeatIds.size() != requestedSeatIds.size()) {
            throw new IllegalArgumentException(
                    "Duplicate seat IDs are not allowed."
            );
        }

        List<Seat> selectedSeats =
                seatRepository.findAllByIdInForUpdate(requestedSeatIds);

        if (selectedSeats.size() != requestedSeatIds.size()) {
            throw new ResourceNotFoundException(
                    "One or more selected seats do not exist."
            );
        }

        for (Seat seat : selectedSeats) {
            if (!seat.isActive()) {
                throw new IllegalArgumentException(
                        "Seat " + seat.getRowLabel()
                                + seat.getSeatNumber() + " is inactive."
                );
            }

            if (!seat.getScreen().getId().equals(show.getScreen().getId())) {
                throw new IllegalArgumentException(
                        "Selected seat does not belong to the show's screen."
                );
            }
        }

        Set<Long> reservedSeatIds = new HashSet<>(
                seatReservationRepository.findReservedSeatIdsByShowId(show.getId())
        );

        for (Seat seat : selectedSeats) {
            if (reservedSeatIds.contains(seat.getId())) {
                throw new IllegalStateException(
                        "Seat " + seat.getRowLabel()
                                + seat.getSeatNumber() + " is already reserved."
                );
            }
        }

        // Pricing is calculated only on the server from the current admin settings.
        PricingSettings pricingSettings = pricingSettingsRepository.findById(1L)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Pricing settings have not been configured. Please contact the administrator."
                ));

        int seatCount = selectedSeats.size();
        BigDecimal ticketSubtotal = money(
                show.getTicketPrice().multiply(BigDecimal.valueOf(seatCount))
        );

        BigDecimal convenienceFeeAmount;
        if (pricingSettings.getFeeType() == PricingSettings.FeeType.PER_TICKET) {
            convenienceFeeAmount = money(
                    pricingSettings.getConvenienceFee()
                            .multiply(BigDecimal.valueOf(seatCount))
            );
        } else {
            convenienceFeeAmount = money(pricingSettings.getConvenienceFee());
        }

        boolean gstEnabled = Boolean.TRUE.equals(pricingSettings.getGstEnabled());
        boolean gstOnTicket = gstEnabled
                && Boolean.TRUE.equals(pricingSettings.getGstOnTicketPrice());
        boolean gstOnConvenienceFee = gstEnabled
                && Boolean.TRUE.equals(pricingSettings.getGstOnConvenienceFee());

        BigDecimal gstOnTicketAmount = gstOnTicket
                ? percentageOf(ticketSubtotal, pricingSettings.getGstPercentage())
                : ZERO_MONEY;

        BigDecimal gstOnConvenienceFeeAmount = gstOnConvenienceFee
                ? percentageOf(convenienceFeeAmount, pricingSettings.getGstPercentage())
                : ZERO_MONEY;

        BigDecimal gstAmount = money(
                gstOnTicketAmount.add(gstOnConvenienceFeeAmount)
        );

        BigDecimal totalAmount = money(
                ticketSubtotal
                        .add(convenienceFeeAmount)
                        .add(gstAmount)
        );

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(BOOKING_EXPIRATION_MINUTES);

        Booking booking = new Booking();
        booking.setShow(show);
        booking.setCustomer(customer);
        booking.setCustomerName(customer.getFullName());
        booking.setCustomerEmail(customer.getEmail());

        // Persist the exact price components used for this booking.
        booking.setTicketSubtotal(ticketSubtotal);
        booking.setConvenienceFeeAmount(convenienceFeeAmount);
        booking.setGstOnTicketAmount(gstOnTicketAmount);
        booking.setGstOnConvenienceFeeAmount(gstOnConvenienceFeeAmount);
        booking.setGstAmount(gstAmount);
        booking.setTotalAmount(totalAmount);

        // Persist the pricing configuration as an immutable booking-time snapshot.
        booking.setFeeTypeSnapshot(pricingSettings.getFeeType());
        booking.setConvenienceFeeUnitSnapshot(pricingSettings.getConvenienceFee());
        booking.setGstEnabledSnapshot(gstEnabled);
        booking.setGstPercentageSnapshot(pricingSettings.getGstPercentage());
        booking.setGstOnTicketPriceSnapshot(
                Boolean.TRUE.equals(pricingSettings.getGstOnTicketPrice())
        );
        booking.setGstOnConvenienceFeeSnapshot(
                Boolean.TRUE.equals(pricingSettings.getGstOnConvenienceFee())
        );
        booking.setCancellationThresholdHoursSnapshot(
                pricingSettings.getCancellationThresholdHours()
        );
        booking.setCancellationPercentageBeforeThresholdSnapshot(
                pricingSettings.getCancellationPercentageBeforeThreshold()
        );
        booking.setCancellationPercentageWithinThresholdSnapshot(
                pricingSettings.getCancellationPercentageWithinThreshold()
        );
        booking.setCancellationAllowedAfterShowStartsSnapshot(
                pricingSettings.isCancellationAllowedAfterShowStarts()
        );

        booking.setStatus(BookingStatus.PENDING);
        booking.setExpiresAt(expiresAt);

        Booking savedBooking = bookingRepository.save(booking);

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

        // Database uniqueness constraints remain the final concurrency safeguard.
        seatReservationRepository.saveAllAndFlush(reservations);

        return BookingMapper.toResponse(savedBooking, savedBookingSeats);
    }

    private BigDecimal percentageOf(BigDecimal amount, BigDecimal percentage) {
        return money(
                amount.multiply(percentage)
                        .divide(ONE_HUNDRED, 6, RoundingMode.HALF_UP)
        );
    }

    private BigDecimal money(BigDecimal amount) {
        return amount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private Show getValidShow(Long showId) {
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Show with ID " + showId + " does not exist."
                ));

        if (!show.isActive()) {
            throw new IllegalStateException("This show is currently inactive.");
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
            throw new IllegalArgumentException("Booking reference is required.");
        }

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null
                || authentication.getName().equals("anonymousUser")) {
            throw new AccessDeniedException("Authentication is required.");
        }

        Booking booking = bookingRepository
                .findWithDetailsByBookingReference(bookingReference.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found."));

        boolean isAdmin = authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN")
                );

        if (!isAdmin) {
            User customer = booking.getCustomer();

            if (customer == null
                    || !customer.getEmail().equalsIgnoreCase(authentication.getName())) {
                throw new AccessDeniedException(
                        "You are not authorized to access this booking."
                );
            }
        }

        List<BookingSeat> bookingSeats =
                bookingSeatRepository.findByBookingId(booking.getId());

        var latestPayment = paymentRepository
                .findFirstByBookingIdOrderByCreatedAtDesc(booking.getId());

        var paymentStatus = latestPayment
                .map(payment -> payment.getStatus())
                .orElse(null);

        return BookingMapper.toResponse(booking, bookingSeats, paymentStatus);
    }
}
