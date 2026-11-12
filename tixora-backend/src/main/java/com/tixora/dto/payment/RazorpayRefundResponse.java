
package com.tixora.dto.payment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RazorpayRefundResponse(

        String id,

        String entity,

        Long amount,

        String currency,

        @JsonProperty("payment_id")
        String payment_id,

        String status,

        @JsonProperty("created_at")
        Long created_at,

        Map<String, String> notes

) {
}
