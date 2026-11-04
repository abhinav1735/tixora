
package com.tixora.service;

import com.tixora.dto.payment.RazorpayOrderRequest;
import com.tixora.dto.payment.RazorpayOrderResponse;
import com.tixora.dto.payment.RazorpayOrdersResponse;
import com.tixora.dto.payment.RazorpayPaymentResponse;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class RazorpayApiService {

    private final RestClient razorpayRestClient;

    public RazorpayApiService(RestClient razorpayRestClient) {
        this.razorpayRestClient = razorpayRestClient;
    }

    public RazorpayOrderResponse createOrder(
            RazorpayOrderRequest request
    ) {
        return razorpayRestClient.post()
                .uri("/orders")
                .body(request)
                .retrieve()
                .body(RazorpayOrderResponse.class);
    }

    public RazorpayOrderResponse fetchOrder(String orderId) {
        return razorpayRestClient.get()
                .uri("/orders/{orderId}", orderId)
                .retrieve()
                .body(RazorpayOrderResponse.class);
    }

    public RazorpayOrdersResponse fetchOrdersByReceipt(String receipt) {

        if (receipt == null || receipt.isBlank()) {
            throw new IllegalArgumentException(
                    "Receipt cannot be empty"
            );
        }

        return razorpayRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/orders")
                        .queryParam("receipt", receipt)
                        .build())
                .retrieve()
                .body(RazorpayOrdersResponse.class);
    }

    public RazorpayPaymentResponse fetchPayment(String paymentId) {
        return razorpayRestClient.get()
                .uri("/payments/{paymentId}", paymentId)
                .retrieve()
                .body(RazorpayPaymentResponse.class);
    }
}
