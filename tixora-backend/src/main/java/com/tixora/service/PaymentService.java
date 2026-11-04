
package com.tixora.service;

import com.tixora.dto.payment.CreatePaymentResponse;
import com.tixora.dto.payment.RazorpayOrderRequest;
import com.tixora.dto.payment.RazorpayOrderResponse;
import com.tixora.dto.payment.RazorpayOrdersResponse;
import com.tixora.dto.payment.RazorpayPaymentResponse;
import com.tixora.dto.payment.VerifyPaymentRequest;
import com.tixora.dto.payment.VerifyPaymentResponse;
import com.tixora.repository.PaymentRepository;
import com.tixora.dto.payment.FailedPaymentRequest;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.List;

@Service
public class PaymentService {

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

    public CreatePaymentResponse createPaymentOrder(String bookingReference) {

        // Step 1: Prepare payment in a short database transaction.

        PaymentTransactionService.PaymentOrderContext context =
                paymentTransactionService.prepareOrderCreation(bookingReference);

        // Return the existing Razorpay order if already created.

        if (context.orderAlreadyCreated()) {
            return buildPaymentResponse(context);
        }

        // Step 2: Recover an order if a previous creation attempt
        // may have succeeded but its response was not saved.

        if (context.reconciliationRequired()) {
            return recoverExistingOrder(context);
        }

        // Step 3: Create a new Razorpay order.

        long amountInPaise = toPaise(context.amount());

        RazorpayOrderRequest orderRequest = new RazorpayOrderRequest(
                amountInPaise,
                CURRENCY,
                context.receipt(),
                Map.of(
                        "bookingReference", context.bookingReference(),
                        "bookingId", context.bookingId().toString(),
                        "paymentId", context.paymentId().toString()
                )
        );

        // External API call stays outside the database transaction.

        RazorpayOrderResponse orderResponse =
                razorpayApiService.createOrder(orderRequest);

        validateRecoveredOrder(context, orderResponse);

        // Step 4: Save Razorpay order ID in a short transaction.

        return paymentTransactionService.completeOrderCreation(
                context.paymentId(),
                context.bookingReference(),
                orderResponse
        );
    }

    private CreatePaymentResponse recoverExistingOrder(
            PaymentTransactionService.PaymentOrderContext context
    ) {

        // Search Razorpay using the persisted unique receipt.

        RazorpayOrdersResponse response =
                razorpayApiService.fetchOrdersByReceipt(context.receipt());

        if (response == null || response.items() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Unable to retrieve orders from Razorpay. Please retry later."
            );
        }

        List<RazorpayOrderResponse> matchingOrders = response.items()
                .stream()
                .filter(order -> order != null)
                .filter(order -> context.receipt().equals(order.receipt()))
                .toList();

        // No order found. Do not blindly create another order.
        if (matchingOrders.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No Razorpay order was found for this receipt. " +
                            "The payment attempt requires reconciliation before retrying."
            );
        }

        // Multiple orders indicate an unexpected gateway state.
        if (matchingOrders.size() > 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Multiple Razorpay orders were found for this receipt. " +
                            "Manual reconciliation is required."
            );
        }

        RazorpayOrderResponse recoveredOrder = matchingOrders.get(0);

        // Validate the recovered gateway order before saving it.
        validateRecoveredOrder(context, recoveredOrder);

        return paymentTransactionService.completeOrderCreation(
                context.paymentId(),
                context.bookingReference(),
                recoveredOrder
        );
    }

    private void validateRecoveredOrder(
            PaymentTransactionService.PaymentOrderContext context,
            RazorpayOrderResponse order
    ) {

        if (order == null ||
                order.id() == null ||
                order.id().isBlank() ||
                order.amount() == null ||
                order.amount() != toPaise(context.amount()) ||
                !CURRENCY.equals(order.currency()) ||
                !context.receipt().equals(order.receipt())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Razorpay order validation failed"
            );
        }
    }

    private CreatePaymentResponse buildPaymentResponse(
            PaymentTransactionService.PaymentOrderContext context
    ) {
        return new CreatePaymentResponse(
                context.bookingReference(),
                context.razorpayOrderId(),
                getPublicKey(),
                context.amount(),
                context.currency(),
                context.customerName(),
                context.customerEmail()
        );
    }

    private long toPaise(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.UNNECESSARY)
                .longValueExact();
    }

    public VerifyPaymentResponse verifyPayment(VerifyPaymentRequest request) {

        // Step 1: Read payment and booking without acquiring a database lock.

        var initialPayment = paymentRepository
                .findWithBookingByRazorpayOrderId(request.razorpayOrderId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Payment order not found"
                ));

        String bookingReference =
                initialPayment.getBooking().getBookingReference();

        // Step 2: Verify signature outside the database transaction.

        boolean signatureValid = razorpaySignatureService.verifySignature(
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

        // Step 3: Fetch payment details from Razorpay outside the transaction.

        RazorpayPaymentResponse gatewayPayment =
                razorpayApiService.fetchPayment(request.razorpayPaymentId());

        if (gatewayPayment == null ||
                !request.razorpayPaymentId().equals(gatewayPayment.id()) ||
                !request.razorpayOrderId().equals(gatewayPayment.orderId()) ||
                !Boolean.TRUE.equals(gatewayPayment.captured()) ||
                !"captured".equalsIgnoreCase(gatewayPayment.status())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payment is not successfully captured"
            );
        }

        // Step 4: Complete verification in a short database transaction.

        return paymentTransactionService.completePaymentVerification(
                request,
                bookingReference,
                gatewayPayment.amount(),
                gatewayPayment.currency()
        );
    }


    public VerifyPaymentResponse handleFailedPayment(
            FailedPaymentRequest request
    ) {

        // 1. Find the payment and its associated booking.
        var payment = paymentRepository
                .findWithBookingByRazorpayOrderId(request.razorpayOrderId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Payment order not found"
                ));

        String bookingReference =
                payment.getBooking().getBookingReference();

        // 2. Retrieve the payment directly from Razorpay.
        RazorpayPaymentResponse gatewayPayment =
                razorpayApiService.fetchPayment(
                        request.razorpayPaymentId()
                );

        // 3. Validate gateway response.
        if (gatewayPayment == null ||
                !request.razorpayPaymentId().equals(gatewayPayment.id()) ||
                !request.razorpayOrderId().equals(gatewayPayment.orderId())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid Razorpay payment details"
            );
        }

        // 4. Only process an actual failed payment.
        if (!"failed".equalsIgnoreCase(gatewayPayment.status())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Razorpay has not confirmed this payment as failed"
            );
        }

        // 5. Complete the database transaction.
        return paymentTransactionService.completeFailedPayment(
                request,
                bookingReference,
                gatewayPayment.amount(),
                gatewayPayment.currency()
        );
    }


    private String getPublicKey() {
        return paymentTransactionService.getPublicKey();
    }
}
