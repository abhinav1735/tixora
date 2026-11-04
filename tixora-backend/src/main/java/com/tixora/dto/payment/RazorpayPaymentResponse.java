
package com.tixora.dto.payment;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RazorpayPaymentResponse(
        String id,

        @JsonProperty("order_id")
        String orderId,

        Long amount,

        String currency,

        String status,

        Boolean captured
) {
}
