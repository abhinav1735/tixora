
package com.tixora.service;

import com.tixora.config.RazorpayProperties;
import com.tixora.dto.payment.CreatePaymentResponse;
import com.tixora.dto.payment.FailedPaymentRequest;
import com.tixora.dto.payment.RazorpayOrderResponse;
import com.tixora.dto.payment.VerifyPaymentRequest;
import com.tixora.dto.payment.VerifyPaymentResponse;
import com.tixora.entity.Booking;
import com.tixora.entity.BookingStatus;
import com.tixora.entity.Payment;
import com.tixora.entity.PaymentStatus;
import com.tixora.repository.BookingRepository;
import com.tixora.repository.PaymentRepository;
import com.tixora.repository.SeatReservationRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PaymentTransactionService {

    private static final String CURRENCY = "INR";

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final SeatReservationRepository seatReservationRepository;
    private final RazorpayProperties razorpayProperties;

    public PaymentTransactionService(
            BookingRepository bookingRepository,
            PaymentRepository paymentRepository,
            SeatReservationRepository seatReservationRepository,
            RazorpayProperties razorpayProperties
    ) {
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.seatReservationRepository = seatReservationRepository;
        this.razorpayProperties = razorpayProperties;
    }

    @Transactional
    public PaymentOrderContext prepareOrderCreation(String bookingReference) {

        Booking booking = bookingRepository
                .findByBookingReferenceForUpdate(bookingReference)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Booking not found"
                ));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Booking is not pending"
            );
        }

        if (booking.getExpiresAt() == null ||
                !booking.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.GONE,
                    "Booking has expired"
            );
        }

        Payment latestPayment = paymentRepository
                .findFirstByBookingIdOrderByCreatedAtDesc(booking.getId())
                .orElse(null);

        if (latestPayment != null) {

            if (latestPayment.getStatus() == PaymentStatus.CREATED &&
                    latestPayment.getRazorpayOrderId() != null) {

                return createContext(
                        booking, latestPayment, true, false
                );
            }

            if (latestPayment.getStatus() == PaymentStatus.ORDER_CREATING) {

                return createContext(
                        booking, latestPayment, false, true
                );
            }

            if (latestPayment.getStatus() == PaymentStatus.CAPTURED) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Payment has already been captured"
                );
            }

            if (latestPayment.getStatus() != PaymentStatus.FAILED) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "A previous payment attempt requires reconciliation"
                );
            }
        }

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(booking.getTotalAmount());
        payment.setCurrency(CURRENCY);
        payment.setStatus(PaymentStatus.ORDER_CREATING);

        String receipt = "TX-" +
                UUID.randomUUID().toString().replace("-", "");

        payment.setReceipt(receipt);

        payment = paymentRepository.saveAndFlush(payment);

        return createContext(
                booking, payment, false, false
        );
    }

    @Transactional
    public CreatePaymentResponse completeOrderCreation(
            Long paymentId,
            String bookingReference,
            RazorpayOrderResponse orderResponse
    ) {

        // Always acquire locks in this order: Booking -> Payment.

        Booking booking = bookingRepository
                .findByBookingReferenceForUpdate(bookingReference)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Booking not found"
                ));

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Payment record not found"
                ));

        if (!payment.getBooking().getId().equals(booking.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Payment does not belong to this booking"
            );
        }

        if (orderResponse == null ||
                orderResponse.id() == null ||
                orderResponse.id().isBlank() ||
                orderResponse.amount() == null ||
                orderResponse.amount() != toPaise(payment.getAmount()) ||
                !CURRENCY.equals(orderResponse.currency()) ||
                !payment.getReceipt().equals(orderResponse.receipt())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Invalid Razorpay order response"
            );
        }

        if (payment.getStatus() == PaymentStatus.CREATED) {

            if (orderResponse.id().equals(payment.getRazorpayOrderId())) {
                return buildPaymentResponse(booking, payment);
            }

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A different Razorpay order is already associated with this payment"
            );
        }

        if (payment.getStatus() != PaymentStatus.ORDER_CREATING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Payment order creation is no longer in progress"
            );
        }

        payment.setRazorpayOrderId(orderResponse.id());
        payment.setStatus(PaymentStatus.CREATED);

        return buildPaymentResponse(booking, payment);
    }

    private CreatePaymentResponse buildPaymentResponse(
            Booking booking,
            Payment payment
    ) {
        return new CreatePaymentResponse(
                booking.getBookingReference(),
                payment.getRazorpayOrderId(),
                razorpayProperties.keyId(),
                payment.getAmount(),
                payment.getCurrency(),
                booking.getCustomerName(),
                booking.getCustomerEmail()
        );
    }

    @Transactional
    public VerifyPaymentResponse completePaymentVerification(
            VerifyPaymentRequest request,
            String bookingReference,
            Long gatewayAmount,
            String gatewayCurrency
    ) {

        // Always acquire locks in this order: Booking -> Payment.

        Booking booking = bookingRepository
                .findByBookingReferenceForUpdate(bookingReference)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Booking not found"
                ));

        Payment payment = paymentRepository
                .findByRazorpayOrderIdForUpdate(request.razorpayOrderId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Payment order not found"
                ));

        if (!payment.getBooking().getId().equals(booking.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Payment does not belong to this booking"
            );
        }

        // Idempotency: same payment ID can be verified again safely.

        if (payment.getStatus() == PaymentStatus.CAPTURED &&
                request.razorpayPaymentId().equals(
                        payment.getRazorpayPaymentId())) {

            return buildVerifyPaymentResponse(
                    payment,
                    booking,
                    "Payment already verified successfully"
            );
        }

        if (payment.getStatus() == PaymentStatus.CAPTURED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This order has already been captured with a different payment ID"
            );
        }

        if (payment.getStatus() != PaymentStatus.CREATED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Payment is not in a verifiable state"
            );
        }

        long expectedAmount = toPaise(payment.getAmount());

        if (gatewayAmount == null ||
                gatewayAmount != expectedAmount ||
                !payment.getCurrency().equals(gatewayCurrency)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payment amount or currency mismatch"
            );
        }

        payment.setRazorpayPaymentId(request.razorpayPaymentId());
        payment.setStatus(PaymentStatus.CAPTURED);

        if (booking.getStatus() != BookingStatus.PENDING ||
                booking.getExpiresAt() == null ||
                !booking.getExpiresAt().isAfter(LocalDateTime.now())) {

            return buildVerifyPaymentResponse(
                    payment,
                    booking,
                    "Payment captured, but booking could not be confirmed. Refund handling is required."
            );
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setExpiresAt(null);

        seatReservationRepository.clearExpiryByBookingId(booking.getId());

        return buildVerifyPaymentResponse(
                payment,
                booking,
                "Payment successful. Booking confirmed."
        );
    }

    // Records a failed payment after gateway verification.
    @Transactional
    public VerifyPaymentResponse completeFailedPayment(
            FailedPaymentRequest request,
            String bookingReference,
            Long gatewayAmount,
            String gatewayCurrency
    ) {

        // Always acquire locks in this order: Booking -> Payment.

        Booking booking = bookingRepository
                .findByBookingReferenceForUpdate(bookingReference)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Booking not found"
                ));

        Payment payment = paymentRepository
                .findByRazorpayOrderIdForUpdate(request.razorpayOrderId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Payment order not found"
                ));

        if (!payment.getBooking().getId().equals(booking.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Payment does not belong to this booking"
            );
        }

        // A late failure callback must never overwrite a successful payment.

        if (payment.getStatus() == PaymentStatus.CAPTURED) {
            return buildVerifyPaymentResponse(
                    payment,
                    booking,
                    "Payment was already captured. Failure callback ignored."
            );
        }

        // Handle duplicate failure callbacks.

        if (payment.getStatus() == PaymentStatus.FAILED) {

            if (request.razorpayPaymentId().equals(
                    payment.getRazorpayPaymentId())) {

                return buildVerifyPaymentResponse(
                        payment,
                        booking,
                        "Payment failure already recorded."
                );
            }

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A different failed payment is already recorded"
            );
        }

        if (payment.getStatus() != PaymentStatus.CREATED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Payment is not in a valid state for failure processing"
            );
        }

        // Validate amount and currency.

        long expectedAmount = toPaise(payment.getAmount());

        if (gatewayAmount == null ||
                gatewayAmount != expectedAmount ||
                !payment.getCurrency().equals(gatewayCurrency)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payment amount or currency mismatch"
            );
        }

        payment.setRazorpayPaymentId(request.razorpayPaymentId());
        payment.setStatus(PaymentStatus.FAILED);

        // Booking remains PENDING.
        // Seat reservations remain unchanged.

        return buildVerifyPaymentResponse(
                payment,
                booking,
                "Failed payment attempt recorded. Booking remains unconfirmed."
        );
    }

    private PaymentOrderContext createContext(
            Booking booking,
            Payment payment,
            boolean orderAlreadyCreated,
            boolean reconciliationRequired
    ) {
        return new PaymentOrderContext(
                payment.getId(),
                booking.getBookingReference(),
                payment.getReceipt(),
                booking.getId(),
                payment.getAmount(),
                payment.getCurrency(),
                booking.getCustomerName(),
                booking.getCustomerEmail(),
                payment.getRazorpayOrderId(),
                orderAlreadyCreated,
                reconciliationRequired
        );
    }

    private long toPaise(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.UNNECESSARY)
                .longValueExact();
    }

    private VerifyPaymentResponse buildVerifyPaymentResponse(
            Payment payment,
            Booking booking,
            String message
    ) {
        return new VerifyPaymentResponse(
                booking.getBookingReference(),
                payment.getRazorpayOrderId(),
                payment.getRazorpayPaymentId(),
                payment.getStatus(),
                booking.getStatus(),
                payment.getAmount(),
                message
        );
    }

    public String getPublicKey() {
        return razorpayProperties.keyId();
    }

    public record PaymentOrderContext(
            Long paymentId,
            String bookingReference,
            String receipt,
            Long bookingId,
            BigDecimal amount,
            String currency,
            String customerName,
            String customerEmail,
            String razorpayOrderId,
            boolean orderAlreadyCreated,
            boolean reconciliationRequired
    ) {
    }
}
