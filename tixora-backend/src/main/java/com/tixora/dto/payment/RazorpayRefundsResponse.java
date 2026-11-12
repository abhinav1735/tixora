
package com.tixora.dto.payment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RazorpayRefundsResponse(

        String entity,

        Integer count,

        List<RazorpayRefundResponse> items

) {
}
