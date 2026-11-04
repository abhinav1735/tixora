package com.tixora.dto.payment;

import java.util.Map;

public record RazorpayOrderRequest(
        long amount,
        String currency,
        String receipt,
        Map<String, String> notes
) {
}