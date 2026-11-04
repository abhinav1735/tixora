
package com.tixora.controller;

import com.tixora.dto.payment.CreatePaymentRequest;
import com.tixora.dto.payment.CreatePaymentResponse;
import com.tixora.dto.payment.VerifyPaymentRequest;
import com.tixora.dto.payment.VerifyPaymentResponse;
import com.tixora.service.PaymentService;
import com.tixora.dto.payment.FailedPaymentRequest;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/create-order")
    public ResponseEntity<CreatePaymentResponse> createPaymentOrder(
            @Valid @RequestBody CreatePaymentRequest request
    ) {
        CreatePaymentResponse response =
                paymentService.createPaymentOrder(
                        request.bookingReference()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<VerifyPaymentResponse> verifyPayment(
            @Valid @RequestBody VerifyPaymentRequest request
    ) {
        VerifyPaymentResponse response =
                paymentService.verifyPayment(request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/failure")
    public ResponseEntity<VerifyPaymentResponse> handleFailedPayment(
            @Valid @RequestBody FailedPaymentRequest request) {

        VerifyPaymentResponse response =
                paymentService.handleFailedPayment(request);

        return ResponseEntity.ok(response);
    }
}
