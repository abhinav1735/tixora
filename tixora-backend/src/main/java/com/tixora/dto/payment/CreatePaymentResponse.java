package com.tixora.dto.payment;

import java.math.BigDecimal;

public record CreatePaymentResponse(

        String bookingReference,

        String razorpayOrderId,

        String razorpayKeyId,

        BigDecimal amount,

        String currency,

        String customerName,

        String customerEmail

) {
}