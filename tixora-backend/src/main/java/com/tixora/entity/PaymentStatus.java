package com.tixora.entity;

public enum PaymentStatus {
    ORDER_CREATING,
    CREATED,
    AUTHORIZED,
    CAPTURED,
    FAILED,
    REFUND_PENDING,
    REFUND_FAILED,
    REFUNDED
}
