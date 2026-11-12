
package com.tixora.service;

import com.tixora.config.RazorpayProperties;
import com.tixora.dto.payment.CreatePaymentResponse;
import com.tixora.dto.payment.FailedPaymentRequest;
import com.tixora.dto.payment.RazorpayOrderResponse;
import com.tixora.dto.payment.RazorpayRefundResponse;
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
                        HttpStatus.NOT_FOUND, "Booking not found"));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Booking is not pending");
        }

        if (booking.getExpiresAt() == null ||
                !booking.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.GONE, "Booking has expired");
        }

        Payment latestPayment = paymentRepository
                .findFirstByBookingIdOrderByCreatedAtDesc(booking.getId())
                .orElse(null);

        if (latestPayment != null) {

            if (latestPayment.getStatus() == PaymentStatus.CREATED &&
                    latestPayment.getRazorpayOrderId() != null) {

                return createContext(
                        booking, latestPayment, true, false);
            }

            if (latestPayment.getStatus() == PaymentStatus.ORDER_CREATING) {
                return createContext(
                        booking, latestPayment, false, true);
            }

            if (latestPayment.getStatus() == PaymentStatus.CAPTURED ||
                    latestPayment.getStatus() == PaymentStatus.REFUND_PENDING ||
                    latestPayment.getStatus() == PaymentStatus.REFUNDED) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Payment has already been captured or refunded");
            }

            if (latestPayment.getStatus() != PaymentStatus.FAILED &&
                    latestPayment.getStatus() != PaymentStatus.REFUND_FAILED) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "A previous payment attempt requires reconciliation");
            }
        }

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(booking.getTotalAmount());
        payment.setCurrency(CURRENCY);
        payment.setStatus(PaymentStatus.ORDER_CREATING);
        payment.setReceipt(
                "TX-" + UUID.randomUUID().toString().replace("-", "")
        );

        payment = paymentRepository.saveAndFlush(payment);

        return createContext(booking, payment, false, false);
    }

    @Transactional
    public CreatePaymentResponse completeOrderCreation(
            Long paymentId,
            String bookingReference,
            RazorpayOrderResponse orderResponse
    ) {

        Booking booking = bookingRepository
                .findByBookingReferenceForUpdate(bookingReference)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Booking not found"));

        Payment payment = paymentRepository
                .findByIdForUpdate(paymentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Payment record not found"));

        if (!payment.getBooking().getId().equals(booking.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Payment does not belong to this booking");
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
                    "Invalid Razorpay order response");
        }

        if (payment.getStatus() == PaymentStatus.CREATED) {

            if (orderResponse.id().equals(payment.getRazorpayOrderId())) {
                return buildPaymentResponse(booking, payment);
            }

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A different Razorpay order is already associated with this payment");
        }

        if (payment.getStatus() != PaymentStatus.ORDER_CREATING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Payment order creation is no longer in progress");
        }

        payment.setRazorpayOrderId(orderResponse.id());
        payment.setStatus(PaymentStatus.CREATED);

        return buildPaymentResponse(booking, payment);
    }

    /*
     * Mark an order-creation attempt as failed only when Razorpay has
     * definitively rejected the request. Uncertain network/server failures
     * must remain ORDER_CREATING so the receipt can be reconciled first.
     *
     * The status check also prevents a late failure response from overwriting
     * an order that was already recovered and saved as CREATED.
     */
    @Transactional
    public void markOrderCreationFailed(Long paymentId) {

        Payment payment = paymentRepository
                .findByIdForUpdate(paymentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Payment record not found"));

        if (payment.getStatus() == PaymentStatus.ORDER_CREATING) {
            payment.setStatus(PaymentStatus.FAILED);
        }
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

        Booking booking = bookingRepository
                .findByBookingReferenceForUpdate(bookingReference)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Booking not found"));

        Payment payment = paymentRepository
                .findByRazorpayOrderIdForUpdate(request.razorpayOrderId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Payment order not found"));

        if (!payment.getBooking().getId().equals(booking.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Payment does not belong to this booking");
        }

        if ((payment.getStatus() == PaymentStatus.CAPTURED ||
                payment.getStatus() == PaymentStatus.REFUND_PENDING ||
                payment.getStatus() == PaymentStatus.REFUND_FAILED ||
                payment.getStatus() == PaymentStatus.REFUNDED) &&
                request.razorpayPaymentId().equals(
                        payment.getRazorpayPaymentId())) {

            String message = switch (payment.getStatus()) {
                case REFUNDED ->
                        "Payment was captured and refunded.";

                case REFUND_PENDING ->
                        "Payment was captured. Refund is pending.";

                case REFUND_FAILED ->
                        "Payment was captured, but refund requires attention.";

                default ->
                        "Payment already verified successfully.";
            };

            return buildVerifyPaymentResponse(
                    payment, booking, message);
        }

        if (payment.getStatus() != PaymentStatus.CREATED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Payment is not in a verifiable state");
        }

        validateGatewayAmount(
                payment, gatewayAmount, gatewayCurrency);

        payment.setRazorpayPaymentId(request.razorpayPaymentId());
        payment.setStatus(PaymentStatus.CAPTURED);

        boolean bookingCanBeConfirmed =
                booking.getStatus() == BookingStatus.PENDING &&
                        booking.getExpiresAt() != null &&
                        booking.getExpiresAt().isAfter(LocalDateTime.now());

        if (!bookingCanBeConfirmed) {
            return buildVerifyPaymentResponse(
                    payment,
                    booking,
                    "Payment captured, but booking could not be confirmed. Refund handling is required."
            );
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setExpiresAt(null);

        seatReservationRepository.clearExpiryByBookingId(
                booking.getId());

        return buildVerifyPaymentResponse(
                payment,
                booking,
                "Payment successful. Booking confirmed."
        );
    }

    /*
     * Marks the payment REFUND_PENDING before calling Razorpay.
     * This prevents repeated verification from blindly creating
     * multiple refunds.
     */
    @Transactional
    public RefundContext prepareExpiredBookingRefund(
            String bookingReference,
            String orderId
    ) {

        Booking booking = bookingRepository
                .findByBookingReferenceForUpdate(bookingReference)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Booking not found"));

        Payment payment = paymentRepository
                .findByRazorpayOrderIdForUpdate(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Payment order not found"));

        if (!payment.getBooking().getId().equals(booking.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Payment does not belong to this booking");
        }

        /*
         * These states must not trigger another refund POST.
         * REFUND_PENDING is reconciled against Razorpay instead.
         */
        if (payment.getStatus() == PaymentStatus.REFUND_PENDING ||
                payment.getStatus() == PaymentStatus.REFUNDED ||
                payment.getStatus() == PaymentStatus.REFUND_FAILED) {

            return new RefundContext(
                    payment.getId(),
                    payment.getRazorpayPaymentId(),
                    payment.getRefundAmount(),
                    payment.getStatus(),
                    false
            );
        }

        LocalDateTime now = LocalDateTime.now();

        /*
         * A booking is expired only when:
         * 1. Its status is EXPIRED or CANCELLED, or
         * 2. It is still PENDING and its expiry time has passed.
         *
         * A CONFIRMED booking has expiresAt = null.
         * Therefore, null alone must never mean expired.
         */
        boolean bookingExpired =
                booking.getStatus() == BookingStatus.EXPIRED ||
                        booking.getStatus() == BookingStatus.CANCELLED ||
                        (booking.getStatus() == BookingStatus.PENDING &&
                                (booking.getExpiresAt() == null ||
                                        !booking.getExpiresAt().isAfter(now)));

        if (!bookingExpired ||
                payment.getStatus() != PaymentStatus.CAPTURED) {
            return null;
        }

        payment.setStatus(PaymentStatus.REFUND_PENDING);
        payment.setRefundAmount(payment.getAmount());
        payment.setRefundInitiatedAt(now);
        payment.setRefundUpdatedAt(now);
        payment.setRefundFailureReason(null);

        return new RefundContext(
                payment.getId(),
                payment.getRazorpayPaymentId(),
                payment.getRefundAmount(),
                PaymentStatus.REFUND_PENDING,
                true
        );
    }

    @Transactional
    public void completeRefund(
            Long paymentId,
            RazorpayRefundResponse refundResponse
    ) {

        Payment payment = paymentRepository
                .findByIdForUpdate(paymentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Payment record not found"));

        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            return;
        }

        if (refundResponse == null ||
                refundResponse.id() == null ||
                refundResponse.id().isBlank() ||
                payment.getRazorpayPaymentId() == null ||
                !payment.getRazorpayPaymentId().equals(
                        refundResponse.payment_id()) ||
                refundResponse.amount() == null ||
                payment.getRefundAmount() == null ||
                refundResponse.amount() != toPaise(
                        payment.getRefundAmount())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Razorpay refund response validation failed");
        }

        payment.setRazorpayRefundId(refundResponse.id());
        payment.setRefundUpdatedAt(LocalDateTime.now());

        if ("processed".equalsIgnoreCase(refundResponse.status())) {

            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setRefundFailureReason(null);

        } else if ("failed".equalsIgnoreCase(refundResponse.status())) {

            payment.setStatus(PaymentStatus.REFUND_FAILED);
            payment.setRefundFailureReason(
                    "Razorpay reported that the refund failed.");

        } else if ("pending".equalsIgnoreCase(refundResponse.status()) ||
                "created".equalsIgnoreCase(refundResponse.status())) {

            payment.setStatus(PaymentStatus.REFUND_PENDING);
            payment.setRefundFailureReason(null);

        } else {

            payment.setStatus(PaymentStatus.REFUND_PENDING);
            payment.setRefundFailureReason(
                    "Unrecognized Razorpay refund status: " +
                            refundResponse.status());
        }
    }

    @Transactional(readOnly = true)
    public VerifyPaymentResponse getLatestVerificationResponse(
            String bookingReference,
            String orderId,
            String message
    ) {

        Booking booking = bookingRepository
                .findByBookingReference(bookingReference)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Booking not found"));

        Payment payment = paymentRepository
                .findByRazorpayOrderId(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Payment order not found"));

        if (!payment.getBooking().getId().equals(booking.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Payment does not belong to this booking");
        }

        String latestMessage = switch (payment.getStatus()) {

            case REFUND_PENDING ->
                    "Payment captured. Refund is pending with Razorpay.";

            case REFUNDED ->
                    "Payment captured and full refund processed.";

            case REFUND_FAILED ->
                    "Payment captured, but refund failed and requires attention.";

            case CAPTURED ->
                    booking.getStatus() == BookingStatus.CONFIRMED
                            ? "Payment successful. Booking confirmed."
                            : message;

            default -> message;
        };

        return buildVerifyPaymentResponse(
                payment, booking, latestMessage);
    }

    @Transactional
    public VerifyPaymentResponse completeFailedPayment(
            FailedPaymentRequest request,
            String bookingReference,
            Long gatewayAmount,
            String gatewayCurrency
    ) {

        Booking booking = bookingRepository
                .findByBookingReferenceForUpdate(bookingReference)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Booking not found"));

        Payment payment = paymentRepository
                .findByRazorpayOrderIdForUpdate(request.razorpayOrderId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Payment order not found"));

        if (!payment.getBooking().getId().equals(booking.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Payment does not belong to this booking");
        }

        if (payment.getStatus() == PaymentStatus.CAPTURED ||
                payment.getStatus() == PaymentStatus.REFUND_PENDING ||
                payment.getStatus() == PaymentStatus.REFUNDED) {

            return buildVerifyPaymentResponse(
                    payment,
                    booking,
                    "Payment was already captured. Failure callback ignored."
            );
        }

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
                    "A different failed payment is already recorded");
        }

        if (payment.getStatus() != PaymentStatus.CREATED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Payment is not in a valid state for failure processing");
        }

        validateGatewayAmount(
                payment, gatewayAmount, gatewayCurrency);

        payment.setRazorpayPaymentId(request.razorpayPaymentId());
        payment.setStatus(PaymentStatus.FAILED);

        return buildVerifyPaymentResponse(
                payment,
                booking,
                "Failed payment attempt recorded. Booking remains unconfirmed."
        );
    }


    /**
     * Applies a verified payment.captured webhook.
     * Returns a refund context only when the booking can no longer be confirmed.
     */
    @Transactional
    public RefundContext handleCapturedWebhook(
            String orderId,
            String paymentId,
            Long gatewayAmount,
            String gatewayCurrency
    ) {
        Payment payment = paymentRepository
                .findByRazorpayOrderIdForUpdate(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Payment order not found"));

        Booking booking = bookingRepository
                .findByBookingReferenceForUpdate(
                        payment.getBooking().getBookingReference())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Booking not found"));

        if (!payment.getBooking().getId().equals(booking.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Payment does not belong to booking");
        }

        if (payment.getStatus() == PaymentStatus.REFUND_PENDING
                || payment.getStatus() == PaymentStatus.REFUNDED
                || payment.getStatus() == PaymentStatus.REFUND_FAILED) {
            return new RefundContext(
                    payment.getId(), payment.getRazorpayPaymentId(),
                    payment.getRefundAmount(), payment.getStatus(), false);
        }

        if (payment.getStatus() == PaymentStatus.CAPTURED) {
            if (!paymentId.equals(payment.getRazorpayPaymentId())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "Different payment already captured");
            }
        } else {
            if (payment.getStatus() != PaymentStatus.CREATED) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "Payment is not in a capturable state");
            }
            validateGatewayAmount(payment, gatewayAmount, gatewayCurrency);
            payment.setRazorpayPaymentId(paymentId);
            payment.setStatus(PaymentStatus.CAPTURED);
        }

        LocalDateTime now = LocalDateTime.now();
        boolean bookingCanBeConfirmed =
                booking.getStatus() == BookingStatus.PENDING
                        && booking.getExpiresAt() != null
                        && booking.getExpiresAt().isAfter(now);

        if (bookingCanBeConfirmed) {
            booking.setStatus(BookingStatus.CONFIRMED);
            booking.setExpiresAt(null);
            seatReservationRepository.clearExpiryByBookingId(booking.getId());
            return null;
        }

        boolean bookingExpired =
                booking.getStatus() == BookingStatus.EXPIRED
                        || booking.getStatus() == BookingStatus.CANCELLED
                        || (booking.getStatus() == BookingStatus.PENDING
                        && (booking.getExpiresAt() == null
                        || !booking.getExpiresAt().isAfter(now)));

        if (!bookingExpired) {
            return null;
        }

        payment.setStatus(PaymentStatus.REFUND_PENDING);
        payment.setRefundAmount(payment.getAmount());
        payment.setRefundInitiatedAt(now);
        payment.setRefundUpdatedAt(now);
        payment.setRefundFailureReason(null);

        return new RefundContext(
                payment.getId(), payment.getRazorpayPaymentId(),
                payment.getRefundAmount(), PaymentStatus.REFUND_PENDING, true);
    }

    /**
     * Applies a verified full-refund webhook to the matching local payment.
     * Partial/unknown refunds are deliberately not mapped to REFUNDED.
     */
    @Transactional
    public void handleRefundWebhook(
            String razorpayPaymentId,
            String razorpayRefundId,
            Long amountInPaise,
            String gatewayStatus
    ) {
        Payment payment = paymentRepository
                .findByRazorpayPaymentIdForUpdate(razorpayPaymentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Payment not found for refund webhook"));

        if (razorpayRefundId == null || razorpayRefundId.isBlank()
                || amountInPaise == null || payment.getRefundAmount() == null
                || amountInPaise != toPaise(payment.getRefundAmount())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Refund webhook details do not match payment");
        }

        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            return;
        }

        payment.setRazorpayRefundId(razorpayRefundId);
        payment.setRefundUpdatedAt(LocalDateTime.now());

        if ("processed".equalsIgnoreCase(gatewayStatus)) {
            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setRefundFailureReason(null);
        } else if ("failed".equalsIgnoreCase(gatewayStatus)) {
            payment.setStatus(PaymentStatus.REFUND_FAILED);
            payment.setRefundFailureReason("Razorpay reported that the refund failed.");
        } else if ("created".equalsIgnoreCase(gatewayStatus)
                || "pending".equalsIgnoreCase(gatewayStatus)) {
            payment.setStatus(PaymentStatus.REFUND_PENDING);
            payment.setRefundFailureReason(null);
        } else {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Unsupported Razorpay refund status");
        }
    }

    private void validateGatewayAmount(
            Payment payment,
            Long gatewayAmount,
            String gatewayCurrency
    ) {

        if (gatewayAmount == null ||
                gatewayAmount != toPaise(payment.getAmount()) ||
                !payment.getCurrency().equals(gatewayCurrency)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payment amount or currency mismatch");
        }
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
    ) {}

    public record RefundContext(
            Long paymentId,
            String razorpayPaymentId,
            BigDecimal amount,
            PaymentStatus status,
            boolean shouldCallGateway
    ) {}
}
