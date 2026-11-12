
package com.tixora.service;

import com.tixora.dto.payment.RazorpayOrderRequest;
import com.tixora.dto.payment.RazorpayOrderResponse;
import com.tixora.dto.payment.RazorpayOrdersResponse;
import com.tixora.dto.payment.RazorpayPaymentResponse;
import com.tixora.dto.payment.RazorpayRefundResponse;
import com.tixora.dto.payment.RazorpayRefundsResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@Service
public class RazorpayApiService {

    private static final Logger log =
            LoggerFactory.getLogger(RazorpayApiService.class);

    private final RestClient razorpayRestClient;

    public RazorpayApiService(RestClient razorpayRestClient) {
        this.razorpayRestClient = razorpayRestClient;
    }

    public RazorpayOrderResponse createOrder(
            RazorpayOrderRequest request
    ) {

        try {

            return razorpayRestClient.post()
                    .uri("/orders")
                    .body(request)
                    .retrieve()
                    .body(RazorpayOrderResponse.class);

        } catch (RestClientResponseException exception) {

            log.error(
                    "Razorpay order creation rejected. HTTP status: {}, response: {}",
                    exception.getStatusCode(),
                    sanitize(exception.getResponseBodyAsString())
            );

            throw exception;

        } catch (ResourceAccessException exception) {

            log.error(
                    "Uncertain outcome while creating Razorpay order. " +
                            "Network or connection failure: {}",
                    exception.getMessage()
            );

            throw exception;
        }
    }

    public RazorpayOrderResponse fetchOrder(String orderId) {

        validateRequired(orderId, "Razorpay order ID");

        return razorpayRestClient.get()
                .uri("/orders/{orderId}", orderId)
                .retrieve()
                .body(RazorpayOrderResponse.class);
    }

    public RazorpayOrdersResponse fetchOrdersByReceipt(
            String receipt
    ) {

        validateRequired(receipt, "Receipt");

        return razorpayRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/orders")
                        .queryParam("receipt", receipt)
                        .build())
                .retrieve()
                .body(RazorpayOrdersResponse.class);
    }

    public RazorpayPaymentResponse fetchPayment(
            String paymentId
    ) {

        validateRequired(paymentId, "Razorpay payment ID");

        return razorpayRestClient.get()
                .uri("/payments/{paymentId}", paymentId)
                .retrieve()
                .body(RazorpayPaymentResponse.class);
    }

    public RazorpayRefundResponse createFullRefund(
            String paymentId,
            long amountInPaise,
            String tixoraPaymentId,
            String bookingReference
    ) {

        validateRequired(paymentId, "Razorpay payment ID");
        validateRequired(tixoraPaymentId, "Tixora payment ID");
        validateRequired(bookingReference, "Booking reference");

        if (amountInPaise <= 0) {
            throw new IllegalArgumentException(
                    "Refund amount must be positive");
        }

        Map<String, Object> requestBody = Map.of(
                "amount", amountInPaise,
                "notes", Map.of(
                        "tixora_payment_id", tixoraPaymentId,
                        "tixora_booking_reference", bookingReference
                )
        );

        return razorpayRestClient.post()
                .uri("/payments/{paymentId}/refund", paymentId)
                .body(requestBody)
                .retrieve()
                .body(RazorpayRefundResponse.class);
    }

    public RazorpayRefundsResponse fetchRefundsForPayment(
            String paymentId
    ) {

        validateRequired(paymentId, "Razorpay payment ID");

        return razorpayRestClient.get()
                .uri("/payments/{paymentId}/refunds", paymentId)
                .retrieve()
                .body(RazorpayRefundsResponse.class);
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

    private void validateRequired(
            String value,
            String fieldName
    ) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " is required");
        }
    }
}
