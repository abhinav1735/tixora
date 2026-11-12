package com.tixora.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "bookings",
        indexes = {
                @Index(name = "idx_booking_reference", columnList = "booking_reference"),
                @Index(name = "idx_booking_show", columnList = "show_id"),
                @Index(name = "idx_booking_email", columnList = "customer_email"),
                @Index(name = "idx_booking_user", columnList = "user_id"),
                @Index(name = "idx_booking_status_expiry", columnList = "status, expires_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_booking_reference",
                        columnNames = "booking_reference"
                )
        }
)
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "booking_reference",
            nullable = false,
            unique = true,
            updatable = false,
            length = 36
    )
    private String bookingReference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "show_id", nullable = false)
    private Show show;

    // Nullable for compatibility with historical bookings created before user ownership.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User customer;

    @NotBlank
    @Size(max = 100)
    @Column(name = "customer_name", nullable = false, length = 100)
    private String customerName;

    @NotBlank
    @Email
    @Size(max = 150)
    @Column(name = "customer_email", nullable = false, length = 150)
    private String customerEmail;

    // Pricing amounts are persisted at booking creation time.
    @DecimalMin("0.01")
    @Digits(integer = 8, fraction = 2)
    @Column(name = "ticket_subtotal", precision = 10, scale = 2)
    private BigDecimal ticketSubtotal;

    @DecimalMin("0.00")
    @Digits(integer = 8, fraction = 2)
    @Column(name = "convenience_fee_amount", precision = 10, scale = 2)
    private BigDecimal convenienceFeeAmount;

    @DecimalMin("0.00")
    @Digits(integer = 8, fraction = 2)
    @Column(name = "gst_on_ticket_amount", precision = 10, scale = 2)
    private BigDecimal gstOnTicketAmount;

    @DecimalMin("0.00")
    @Digits(integer = 8, fraction = 2)
    @Column(name = "gst_on_convenience_fee_amount", precision = 10, scale = 2)
    private BigDecimal gstOnConvenienceFeeAmount;

    @DecimalMin("0.00")
    @Digits(integer = 8, fraction = 2)
    @Column(name = "gst_amount", precision = 10, scale = 2)
    private BigDecimal gstAmount;

    @NotNull
    @DecimalMin("0.01")
    @Digits(integer = 8, fraction = 2)
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    // Snapshot of the pricing configuration used for this booking.
    // Nullable to preserve rows created before these fields existed.
    @Enumerated(EnumType.STRING)
    @Column(name = "fee_type_snapshot", length = 20)
    private PricingSettings.FeeType feeTypeSnapshot;

    @Column(name = "convenience_fee_unit_snapshot", precision = 10, scale = 2)
    private BigDecimal convenienceFeeUnitSnapshot;

    @Column(name = "gst_enabled_snapshot")
    private Boolean gstEnabledSnapshot;

    @Column(name = "gst_percentage_snapshot", precision = 5, scale = 2)
    private BigDecimal gstPercentageSnapshot;

    @Column(name = "gst_on_ticket_price_snapshot")
    private Boolean gstOnTicketPriceSnapshot;

    @Column(name = "gst_on_convenience_fee_snapshot")
    private Boolean gstOnConvenienceFeeSnapshot;

    // Cancellation policy is also snapshotted so later admin edits do not
    // change the policy applicable to an already-created booking.
    @Column(name = "cancellation_threshold_hours_snapshot")
    private Integer cancellationThresholdHoursSnapshot;

    @Column(name = "cancellation_percentage_before_snapshot", precision = 5, scale = 2)
    private BigDecimal cancellationPercentageBeforeThresholdSnapshot;

    @Column(name = "cancellation_percentage_within_snapshot", precision = 5, scale = 2)
    private BigDecimal cancellationPercentageWithinThresholdSnapshot;

    @Column(name = "cancellation_after_show_starts_snapshot")
    private Boolean cancellationAllowedAfterShowStartsSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status = BookingStatus.PENDING;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Booking() {
    }

    @PrePersist
    protected void onCreate() {
        if (bookingReference == null) {
            bookingReference = UUID.randomUUID().toString();
        }
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

    public String getBookingReference() {
        return bookingReference;
    }

    public Show getShow() {
        return show;
    }

    public void setShow(Show show) {
        this.show = show;
    }

    public User getCustomer() {
        return customer;
    }

    public void setCustomer(User customer) {
        this.customer = customer;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public BigDecimal getTicketSubtotal() {
        return ticketSubtotal;
    }

    public void setTicketSubtotal(BigDecimal ticketSubtotal) {
        this.ticketSubtotal = ticketSubtotal;
    }

    public BigDecimal getConvenienceFeeAmount() {
        return convenienceFeeAmount;
    }

    public void setConvenienceFeeAmount(BigDecimal convenienceFeeAmount) {
        this.convenienceFeeAmount = convenienceFeeAmount;
    }

    public BigDecimal getGstOnTicketAmount() {
        return gstOnTicketAmount;
    }

    public void setGstOnTicketAmount(BigDecimal gstOnTicketAmount) {
        this.gstOnTicketAmount = gstOnTicketAmount;
    }

    public BigDecimal getGstOnConvenienceFeeAmount() {
        return gstOnConvenienceFeeAmount;
    }

    public void setGstOnConvenienceFeeAmount(BigDecimal gstOnConvenienceFeeAmount) {
        this.gstOnConvenienceFeeAmount = gstOnConvenienceFeeAmount;
    }

    public BigDecimal getGstAmount() {
        return gstAmount;
    }

    public void setGstAmount(BigDecimal gstAmount) {
        this.gstAmount = gstAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public PricingSettings.FeeType getFeeTypeSnapshot() {
        return feeTypeSnapshot;
    }

    public void setFeeTypeSnapshot(PricingSettings.FeeType feeTypeSnapshot) {
        this.feeTypeSnapshot = feeTypeSnapshot;
    }

    public BigDecimal getConvenienceFeeUnitSnapshot() {
        return convenienceFeeUnitSnapshot;
    }

    public void setConvenienceFeeUnitSnapshot(BigDecimal convenienceFeeUnitSnapshot) {
        this.convenienceFeeUnitSnapshot = convenienceFeeUnitSnapshot;
    }

    public Boolean getGstEnabledSnapshot() {
        return gstEnabledSnapshot;
    }

    public void setGstEnabledSnapshot(Boolean gstEnabledSnapshot) {
        this.gstEnabledSnapshot = gstEnabledSnapshot;
    }

    public BigDecimal getGstPercentageSnapshot() {
        return gstPercentageSnapshot;
    }

    public void setGstPercentageSnapshot(BigDecimal gstPercentageSnapshot) {
        this.gstPercentageSnapshot = gstPercentageSnapshot;
    }

    public Boolean getGstOnTicketPriceSnapshot() {
        return gstOnTicketPriceSnapshot;
    }

    public void setGstOnTicketPriceSnapshot(Boolean gstOnTicketPriceSnapshot) {
        this.gstOnTicketPriceSnapshot = gstOnTicketPriceSnapshot;
    }

    public Boolean getGstOnConvenienceFeeSnapshot() {
        return gstOnConvenienceFeeSnapshot;
    }

    public void setGstOnConvenienceFeeSnapshot(Boolean gstOnConvenienceFeeSnapshot) {
        this.gstOnConvenienceFeeSnapshot = gstOnConvenienceFeeSnapshot;
    }

    public Integer getCancellationThresholdHoursSnapshot() {
        return cancellationThresholdHoursSnapshot;
    }

    public void setCancellationThresholdHoursSnapshot(Integer value) {
        this.cancellationThresholdHoursSnapshot = value;
    }

    public BigDecimal getCancellationPercentageBeforeThresholdSnapshot() {
        return cancellationPercentageBeforeThresholdSnapshot;
    }

    public void setCancellationPercentageBeforeThresholdSnapshot(BigDecimal value) {
        this.cancellationPercentageBeforeThresholdSnapshot = value;
    }

    public BigDecimal getCancellationPercentageWithinThresholdSnapshot() {
        return cancellationPercentageWithinThresholdSnapshot;
    }

    public void setCancellationPercentageWithinThresholdSnapshot(BigDecimal value) {
        this.cancellationPercentageWithinThresholdSnapshot = value;
    }

    public Boolean getCancellationAllowedAfterShowStartsSnapshot() {
        return cancellationAllowedAfterShowStartsSnapshot;
    }

    public void setCancellationAllowedAfterShowStartsSnapshot(Boolean value) {
        this.cancellationAllowedAfterShowStartsSnapshot = value;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
