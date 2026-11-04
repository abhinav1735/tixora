package com.tixora.dto.payment;

public record RazorpayOrderResponse(
        String id,
        Long amount,
        String currency,
        String receipt,
        String status
) {
}