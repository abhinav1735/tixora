
package com.tixora.dto.payment;

import java.util.List;

public record RazorpayOrdersResponse(
        String entity,
        Integer count,
        List<RazorpayOrderResponse> items
) {
}
