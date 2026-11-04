
package com.tixora.dto.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FailedPaymentRequest(

        @NotBlank(message = "Razorpay order ID is required")
        @Size(max = 100)
        String razorpayOrderId,

        @NotBlank(message = "Razorpay payment ID is required")
        @Size(max = 100)
        String razorpayPaymentId

) {
}
