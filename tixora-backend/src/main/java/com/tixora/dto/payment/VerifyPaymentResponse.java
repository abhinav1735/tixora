package com.tixora.dto.payment;

import com.tixora.entity.BookingStatus;
import com.tixora.entity.PaymentStatus;

import java.math.BigDecimal;

public record VerifyPaymentResponse(

        String bookingReference,

        String razorpayOrderId,

        String razorpayPaymentId,

        PaymentStatus paymentStatus,

        BookingStatus bookingStatus,

        BigDecimal amount,

        String message

) {
}