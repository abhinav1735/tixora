package com.tixora.dto.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePaymentRequest(

        @NotBlank(message = "Booking reference is required")
        @Size(max = 36, message = "Invalid booking reference")
        String bookingReference

) {
}