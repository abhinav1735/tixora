
package com.tixora.service;

import com.tixora.dto.payment.CreatePaymentResponse;
import com.tixora.dto.payment.FailedPaymentRequest;
import com.tixora.dto.payment.RazorpayOrderRequest;
import com.tixora.dto.payment.RazorpayOrderResponse;
import com.tixora.dto.payment.RazorpayOrdersResponse;
import com.tixora.dto.payment.RazorpayPaymentResponse;
import com.tixora.dto.payment.RazorpayRefundResponse;
import com.tixora.dto.payment.RazorpayRefundsResponse;
import com.tixora.dto.payment.VerifyPaymentRequest;
import com.tixora.dto.payment.VerifyPaymentResponse;
import com.tixora.entity.PaymentStatus;
import com.tixora.repository.PaymentRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Service
public class PaymentService {

    private static final Logger log =
            LoggerFactory.getLogger(PaymentService.class);

    private static final String CURRENCY = "INR";

    private final PaymentTransactionService paymentTransactionService;
    private final PaymentRepository paymentRepository;
    private final RazorpayApiService razorpayApiService;
    private final RazorpaySignatureService razorpaySignatureService;

    public PaymentService(
            PaymentTransactionService paymentTransactionService,
            PaymentRepository paymentRepository,
            RazorpayApiService razorpayApiService,
            RazorpaySignatureService razorpaySignatureService
    ) {
        this.paymentTransactionService = paymentTransactionService;
        this.paymentRepository = paymentRepository;
        this.razorpayApiService = razorpayApiService;
        this.razorpaySignatureService = razorpaySignatureService;
    }

    // =========================================================
    // CREATE PAYMENT ORDER
    // =========================================================

    public CreatePaymentResponse createPaymentOrder(
            String bookingReference
    ) {

        PaymentTransactionService.PaymentOrderContext context =
                paymentTransactionService.prepareOrderCreation(
                        bookingReference
                );

        if (context.orderAlreadyCreated()) {
            return buildPaymentResponse(context);
        }

        if (context.reconciliationRequired()) {
            return recoverExistingOrder(context);
        }

        RazorpayOrderRequest orderRequest =
                new RazorpayOrderRequest(
                        toPaise(context.amount()),
                        CURRENCY,
                        context.receipt(),
                        Map.of(
                                "bookingReference",
                                context.bookingReference(),

                                "bookingId",
                                context.bookingId().toString(),

                                "paymentId",
                                context.paymentId().toString()
                        )
                );

        RazorpayOrderResponse orderResponse;

        try {

            orderResponse =
                    razorpayApiService.createOrder(orderRequest);

        } catch (RestClientResponseException exception) {

            int statusCode = exception.getStatusCode().value();

            /*
             * Only definite client-side rejections can close this attempt.
             * Timeouts, conflicts, early-data responses and rate limits can
             * have ambiguous outcomes, so keep ORDER_CREATING for reconciliation.
             * 5xx responses are also treated as uncertain.
             */
            boolean definitiveRejection =
                    exception.getStatusCode().is4xxClientError()
                            && statusCode != 408
                            && statusCode != 409
                            && statusCode != 425
                            && statusCode != 429;

            if (definitiveRejection) {
                try {
                    paymentTransactionService.markOrderCreationFailed(
                            context.paymentId()
                    );
                } catch (RuntimeException stateException) {
                    log.error(
                            "Could not mark definitively rejected order attempt as FAILED. " +
                                    "Payment ID: {}. Reconciliation may be required.",
                            context.paymentId(),
                            stateException
                    );
                }
            }

            log.error(
                    "Razorpay order creation returned HTTP error. " +
                            "Payment ID: {}, HTTP status: {}, definitive rejection: {}, response: {}",
                    context.paymentId(),
                    exception.getStatusCode(),
                    definitiveRejection,
                    sanitize(exception.getResponseBodyAsString())
            );

            throw new ResponseStatusException(
                    definitiveRejection
                            ? HttpStatus.BAD_GATEWAY
                            : HttpStatus.SERVICE_UNAVAILABLE,
                    definitiveRejection
                            ? "Razorpay rejected the order creation request."
                            : "Razorpay order creation outcome is uncertain. " +
                            "Reconciliation is required before retrying."
            );

        } catch (ResourceAccessException exception) {

            log.error(
                    "Uncertain Razorpay order creation outcome. " +
                            "Payment ID: {}, Receipt: {}",
                    context.paymentId(),
                    context.receipt(),
                    exception
            );

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Razorpay connection failed. " +
                            "Order reconciliation is required before retrying."
            );
        }

        validateOrder(context, orderResponse);

        return paymentTransactionService.completeOrderCreation(
                context.paymentId(),
                context.bookingReference(),
                orderResponse
        );
    }

    // =========================================================
    // RECOVER EXISTING RAZORPAY ORDER
    // =========================================================

    private CreatePaymentResponse recoverExistingOrder(
            PaymentTransactionService.PaymentOrderContext context
    ) {

        RazorpayOrdersResponse response;

        try {

            response = razorpayApiService.fetchOrdersByReceipt(
                    context.receipt()
            );

        } catch (RuntimeException exception) {

            log.error(
                    "Unable to reconcile Razorpay order. " +
                            "Payment ID: {}, Receipt: {}",
                    context.paymentId(),
                    context.receipt(),
                    exception
            );

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Unable to reconcile the existing Razorpay order. " +
                            "Please retry reconciliation later."
            );
        }

        if (response == null || response.items() == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Unable to retrieve orders from Razorpay."
            );
        }

        List<RazorpayOrderResponse> matchingOrders =
                response.items()
                        .stream()
                        .filter(order -> order != null)
                        .filter(order ->
                                context.receipt().equals(order.receipt())
                        )
                        .toList();

        if (matchingOrders.size() != 1) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    matchingOrders.isEmpty()
                            ? "No Razorpay order was found for this receipt. " +
                            "Manual reconciliation is required."
                            : "Multiple Razorpay orders were found for this receipt. " +
                            "Manual reconciliation is required."
            );
        }

        RazorpayOrderResponse recoveredOrder =
                matchingOrders.get(0);

        validateOrder(context, recoveredOrder);

        return paymentTransactionService.completeOrderCreation(
                context.paymentId(),
                context.bookingReference(),
                recoveredOrder
        );
    }

    // =========================================================
    // VALIDATE RAZORPAY ORDER
    // =========================================================

    private void validateOrder(
            PaymentTransactionService.PaymentOrderContext context,
            RazorpayOrderResponse order
    ) {

        if (order == null
                || order.id() == null
                || order.id().isBlank()
                || order.amount() == null
                || order.amount() != toPaise(context.amount())
                || !CURRENCY.equals(order.currency())
                || !context.receipt().equals(order.receipt())) {

            log.error(
                    "Razorpay order validation failed. Payment ID: {}",
                    context.paymentId()
            );

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Razorpay order validation failed."
            );
        }
    }

    // =========================================================
    // BUILD PAYMENT RESPONSE
    // =========================================================

    private CreatePaymentResponse buildPaymentResponse(
            PaymentTransactionService.PaymentOrderContext context
    ) {

        return new CreatePaymentResponse(
                context.bookingReference(),
                context.razorpayOrderId(),
                paymentTransactionService.getPublicKey(),
                context.amount(),
                context.currency(),
                context.customerName(),
                context.customerEmail()
        );
    }

    // =========================================================
    // VERIFY PAYMENT
    // =========================================================

    public VerifyPaymentResponse verifyPayment(
            VerifyPaymentRequest request
    ) {

        var initialPayment = paymentRepository
                .findWithBookingByRazorpayOrderId(
                        request.razorpayOrderId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Payment order not found"
                ));

        String bookingReference =
                initialPayment.getBooking().getBookingReference();

        boolean signatureValid =
                razorpaySignatureService.verifySignature(
                        request.razorpayOrderId(),
                        request.razorpayPaymentId(),
                        request.razorpaySignature()
                );

        if (!signatureValid) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid payment signature"
            );
        }

        RazorpayPaymentResponse gatewayPayment =
                razorpayApiService.fetchPayment(
                        request.razorpayPaymentId()
                );

        if (gatewayPayment == null
                || !request.razorpayPaymentId().equals(gatewayPayment.id())
                || !request.razorpayOrderId().equals(gatewayPayment.orderId())
                || !Boolean.TRUE.equals(gatewayPayment.captured())
                || !"captured".equalsIgnoreCase(gatewayPayment.status())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payment is not successfully captured"
            );
        }

        VerifyPaymentResponse verification =
                paymentTransactionService.completePaymentVerification(
                        request,
                        bookingReference,
                        gatewayPayment.amount(),
                        gatewayPayment.currency()
                );

        /*
         * Check whether the captured payment needs a refund.
         * This includes expired and cancelled bookings.
         */

        PaymentTransactionService.RefundContext refundContext =
                paymentTransactionService.prepareExpiredBookingRefund(
                        bookingReference,
                        request.razorpayOrderId()
                );

        if (refundContext != null) {

            if (refundContext.shouldCallGateway()) {

                submitFullRefund(
                        refundContext,
                        bookingReference
                );

            } else if ("REFUND_PENDING".equals(
                    refundContext.status().name())) {

                reconcilePendingRefund(refundContext);
            }
        }

        return paymentTransactionService.getLatestVerificationResponse(
                bookingReference,
                request.razorpayOrderId(),
                verification.message()
        );
    }


    /**
     * Handles a payment.captured webhook after the webhook signature is verified.
     */
    public void handleCapturedWebhook(
            String orderId,
            String paymentId,
            Long amount,
            String currency
    ) {
        PaymentTransactionService.RefundContext context =
                paymentTransactionService.handleCapturedWebhook(
                        orderId, paymentId, amount, currency);

        if (context != null && context.shouldCallGateway()) {
            var payment = paymentRepository.findWithBookingById(context.paymentId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Payment disappeared after webhook processing"));

            String bookingReference =
                    payment.getBooking().getBookingReference();

            submitFullRefund(context, bookingReference);
        }
    }

    // =========================================================
    // SUBMIT FULL REFUND
    // =========================================================

    private void submitFullRefund(
            PaymentTransactionService.RefundContext context,
            String bookingReference
    ) {

        try {

            RazorpayRefundResponse refundResponse =
                    razorpayApiService.createFullRefund(
                            context.razorpayPaymentId(),
                            toPaise(context.amount()),
                            context.paymentId().toString(),
                            bookingReference
                    );

            paymentTransactionService.completeRefund(
                    context.paymentId(),
                    refundResponse
            );

        } catch (RuntimeException exception) {

            /*
             * The refund request might have reached Razorpay
             * even if the response was lost.
             *
             * Keep REFUND_PENDING.
             * Reconciliation must check Razorpay before
             * any further refund action.
             */

            log.error(
                    "Refund submission or response failed. " +
                            "Payment ID: {}. Refund remains pending.",
                    context.paymentId(),
                    exception
            );
        }
    }

    // =========================================================
    // RECONCILE PENDING REFUND
    // =========================================================

    /**
     * Reconcile one pending refund.
     *
     * Called by the scheduled reconciliation job. This method only checks
     * Razorpay's existing refund records; it never creates another refund.
     */
    public void reconcileRefund(Long paymentId) {

        if (paymentId == null) {
            log.warn("Refund reconciliation skipped because payment ID is null.");
            return;
        }

        var payment = paymentRepository.findById(paymentId).orElse(null);

        if (payment == null) {
            log.warn(
                    "Refund reconciliation skipped because payment was not found. Payment ID: {}",
                    paymentId
            );
            return;
        }

        if (payment.getStatus() != PaymentStatus.REFUND_PENDING) {
            return;
        }

        if (payment.getRazorpayPaymentId() == null
                || payment.getRazorpayPaymentId().isBlank()
                || payment.getRefundAmount() == null
                || payment.getRefundAmount().signum() <= 0) {

            log.error(
                    "Pending refund has incomplete reconciliation data. Payment ID: {}",
                    paymentId
            );
            return;
        }

        PaymentTransactionService.RefundContext context =
                new PaymentTransactionService.RefundContext(
                        payment.getId(),
                        payment.getRazorpayPaymentId(),
                        payment.getRefundAmount(),
                        payment.getStatus(),
                        false
                );

        reconcilePendingRefund(context);
    }

    private void reconcilePendingRefund(
            PaymentTransactionService.RefundContext context
    ) {

        try {

            RazorpayRefundsResponse response =
                    razorpayApiService.fetchRefundsForPayment(
                            context.razorpayPaymentId()
                    );

            if (response == null || response.items() == null) {
                return;
            }

            List<RazorpayRefundResponse> matchingRefunds =
                    response.items()
                            .stream()
                            .filter(refund -> refund != null)
                            .filter(refund ->
                                    refund.amount() != null
                                            && refund.amount() == toPaise(
                                            context.amount()
                                    )
                            )
                            .filter(refund ->
                                    context.razorpayPaymentId().equals(refund.payment_id())
                            )
                            .filter(refund ->
                                    refund.notes() != null
                            )
                            .filter(refund ->
                                    context.paymentId()
                                            .toString()
                                            .equals(
                                                    refund.notes().get(
                                                            "tixora_payment_id"
                                                    )
                                            )
                            )
                            .toList();

            if (matchingRefunds.size() == 1) {

                paymentTransactionService.completeRefund(
                        context.paymentId(),
                        matchingRefunds.get(0)
                );
            }

            /*
             * No matching refund:
             * Keep REFUND_PENDING.
             * Do not blindly submit another refund.
             *
             * Multiple matching refunds:
             * Manual reconciliation is required.
             */

            if (matchingRefunds.size() > 1) {

                log.error(
                        "Multiple matching Razorpay refunds found. " +
                                "Payment ID: {}. Manual reconciliation required.",
                        context.paymentId()
                );
            }

        } catch (RuntimeException exception) {

            log.error(
                    "Refund reconciliation failed. Payment ID: {}",
                    context.paymentId(),
                    exception
            );

            // Keep REFUND_PENDING.
        }
    }

    // =========================================================
    // HANDLE FAILED PAYMENT
    // =========================================================

    public VerifyPaymentResponse handleFailedPayment(
            FailedPaymentRequest request
    ) {

        var payment = paymentRepository
                .findWithBookingByRazorpayOrderId(
                        request.razorpayOrderId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Payment order not found"
                ));

        String bookingReference =
                payment.getBooking().getBookingReference();

        RazorpayPaymentResponse gatewayPayment =
                razorpayApiService.fetchPayment(
                        request.razorpayPaymentId()
                );

        if (gatewayPayment == null
                || !request.razorpayPaymentId().equals(gatewayPayment.id())
                || !request.razorpayOrderId().equals(gatewayPayment.orderId())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid Razorpay payment details"
            );
        }

        if (!"failed".equalsIgnoreCase(gatewayPayment.status())) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Razorpay has not confirmed this payment as failed"
            );
        }

        return paymentTransactionService.completeFailedPayment(
                request,
                bookingReference,
                gatewayPayment.amount(),
                gatewayPayment.currency()
        );
    }

    // =========================================================
    // HELPER METHODS
    // =========================================================

    private long toPaise(BigDecimal amount) {

        return amount.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.UNNECESSARY)
                .longValueExact();
    }

    private String sanitize(String responseBody) {

        if (responseBody == null || responseBody.isBlank()) {
            return "No response body";
        }

        int maxLength = 2000;

        return responseBody.length() > maxLength
                ? responseBody.substring(0, maxLength)
                : responseBody;
    }
}
