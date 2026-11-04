
package com.tixora.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pricing_settings")
public class PricingSettings {

    public enum FeeType {
        PER_TICKET,
        PER_BOOKING
    }

    @Id
    private Long id = 1L;

    // Booking convenience fee
    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 8, fraction = 2)
    @Column(name = "convenience_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal convenienceFee;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "fee_type", nullable = false, length = 20)
    private FeeType feeType;

    // GST configuration
    @NotNull
    @Column(name = "gst_enabled", nullable = false)
    private Boolean gstEnabled;

    @NotNull
    @DecimalMin("0.00")
    @DecimalMax("100.00")
    @Digits(integer = 3, fraction = 2)
    @Column(name = "gst_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal gstPercentage;

    @NotNull
    @Column(name = "gst_on_ticket_price", nullable = false)
    private Boolean gstOnTicketPrice;

    @NotNull
    @Column(name = "gst_on_convenience_fee", nullable = false)
    private Boolean gstOnConvenienceFee;

    // Cancellation configuration
    @NotNull
    @Min(0)
    @Column(name = "cancellation_threshold_hours", nullable = false)
    private Integer cancellationThresholdHours;

    @NotNull
    @DecimalMin("0.00")
    @DecimalMax("100.00")
    @Digits(integer = 3, fraction = 2)
    @Column(name = "cancellation_percentage_before_threshold",
            nullable = false, precision = 5, scale = 2)
    private BigDecimal cancellationPercentageBeforeThreshold;

    @NotNull
    @DecimalMin("0.00")
    @DecimalMax("100.00")
    @Digits(integer = 3, fraction = 2)
    @Column(name = "cancellation_percentage_within_threshold",
            nullable = false, precision = 5, scale = 2)
    private BigDecimal cancellationPercentageWithinThreshold;

    // Cancellation after show starts is never permitted.
    @Column(name = "allow_cancellation_after_show_starts",
            nullable = false)
    private final boolean allowCancellationAfterShowStarts = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public PricingSettings() {
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public BigDecimal getConvenienceFee() {
        return convenienceFee;
    }

    public void setConvenienceFee(BigDecimal convenienceFee) {
        this.convenienceFee = convenienceFee;
    }

    public FeeType getFeeType() {
        return feeType;
    }

    public void setFeeType(FeeType feeType) {
        this.feeType = feeType;
    }

    public Boolean getGstEnabled() {
        return gstEnabled;
    }

    public void setGstEnabled(Boolean gstEnabled) {
        this.gstEnabled = gstEnabled;
    }

    public BigDecimal getGstPercentage() {
        return gstPercentage;
    }

    public void setGstPercentage(BigDecimal gstPercentage) {
        this.gstPercentage = gstPercentage;
    }

    public Boolean getGstOnTicketPrice() {
        return gstOnTicketPrice;
    }

    public void setGstOnTicketPrice(Boolean gstOnTicketPrice) {
        this.gstOnTicketPrice = gstOnTicketPrice;
    }

    public Boolean getGstOnConvenienceFee() {
        return gstOnConvenienceFee;
    }

    public void setGstOnConvenienceFee(Boolean gstOnConvenienceFee) {
        this.gstOnConvenienceFee = gstOnConvenienceFee;
    }

    public Integer getCancellationThresholdHours() {
        return cancellationThresholdHours;
    }

    public void setCancellationThresholdHours(Integer cancellationThresholdHours) {
        this.cancellationThresholdHours = cancellationThresholdHours;
    }

    public BigDecimal getCancellationPercentageBeforeThreshold() {
        return cancellationPercentageBeforeThreshold;
    }

    public void setCancellationPercentageBeforeThreshold(
            BigDecimal cancellationPercentageBeforeThreshold) {
        this.cancellationPercentageBeforeThreshold =
                cancellationPercentageBeforeThreshold;
    }

    public BigDecimal getCancellationPercentageWithinThreshold() {
        return cancellationPercentageWithinThreshold;
    }

    public void setCancellationPercentageWithinThreshold(
            BigDecimal cancellationPercentageWithinThreshold) {
        this.cancellationPercentageWithinThreshold =
                cancellationPercentageWithinThreshold;
    }

    public boolean isCancellationAllowedAfterShowStarts() {
        return allowCancellationAfterShowStarts;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
