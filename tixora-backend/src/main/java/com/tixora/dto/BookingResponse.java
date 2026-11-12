package com.tixora.dto;

import com.tixora.entity.BookingStatus;
import com.tixora.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class BookingResponse {

    private Long id;
    private String bookingReference;

    private Long showId;
    private String movieTitle;
    private String theatreName;
    private String screenName;

    private LocalDateTime showStartAt;
    private LocalDateTime showEndAt;
    private BigDecimal ticketPrice;

    private String customerName;
    private String customerEmail;
    private List<String> seats;

    private BigDecimal ticketSubtotal;
    private BigDecimal convenienceFeeAmount;
    private BigDecimal gstOnTicketAmount;
    private BigDecimal gstOnConvenienceFeeAmount;
    private BigDecimal gstAmount;
    private BigDecimal totalAmount;

    private String feeTypeSnapshot;
    private BigDecimal convenienceFeeUnitSnapshot;
    private Boolean gstEnabledSnapshot;
    private BigDecimal gstPercentageSnapshot;
    private Boolean gstOnTicketPriceSnapshot;
    private Boolean gstOnConvenienceFeeSnapshot;

    private Integer cancellationThresholdHoursSnapshot;
    private BigDecimal cancellationPercentageBeforeThresholdSnapshot;
    private BigDecimal cancellationPercentageWithinThresholdSnapshot;
    private Boolean cancellationAllowedAfterShowStartsSnapshot;

    private BookingStatus status;
    private PaymentStatus paymentStatus;
    private LocalDateTime createdAt;

    public BookingResponse(
            Long id,
            String bookingReference,
            Long showId,
            String movieTitle,
            String theatreName,
            String screenName,
            LocalDateTime showStartAt,
            LocalDateTime showEndAt,
            BigDecimal ticketPrice,
            String customerName,
            String customerEmail,
            List<String> seats,
            BigDecimal ticketSubtotal,
            BigDecimal convenienceFeeAmount,
            BigDecimal gstOnTicketAmount,
            BigDecimal gstOnConvenienceFeeAmount,
            BigDecimal gstAmount,
            BigDecimal totalAmount,
            String feeTypeSnapshot,
            BigDecimal convenienceFeeUnitSnapshot,
            Boolean gstEnabledSnapshot,
            BigDecimal gstPercentageSnapshot,
            Boolean gstOnTicketPriceSnapshot,
            Boolean gstOnConvenienceFeeSnapshot,
            Integer cancellationThresholdHoursSnapshot,
            BigDecimal cancellationPercentageBeforeThresholdSnapshot,
            BigDecimal cancellationPercentageWithinThresholdSnapshot,
            Boolean cancellationAllowedAfterShowStartsSnapshot,
            BookingStatus status,
            PaymentStatus paymentStatus,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.bookingReference = bookingReference;
        this.showId = showId;
        this.movieTitle = movieTitle;
        this.theatreName = theatreName;
        this.screenName = screenName;
        this.showStartAt = showStartAt;
        this.showEndAt = showEndAt;
        this.ticketPrice = ticketPrice;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.seats = seats;
        this.ticketSubtotal = ticketSubtotal;
        this.convenienceFeeAmount = convenienceFeeAmount;
        this.gstOnTicketAmount = gstOnTicketAmount;
        this.gstOnConvenienceFeeAmount = gstOnConvenienceFeeAmount;
        this.gstAmount = gstAmount;
        this.totalAmount = totalAmount;
        this.feeTypeSnapshot = feeTypeSnapshot;
        this.convenienceFeeUnitSnapshot = convenienceFeeUnitSnapshot;
        this.gstEnabledSnapshot = gstEnabledSnapshot;
        this.gstPercentageSnapshot = gstPercentageSnapshot;
        this.gstOnTicketPriceSnapshot = gstOnTicketPriceSnapshot;
        this.gstOnConvenienceFeeSnapshot = gstOnConvenienceFeeSnapshot;
        this.cancellationThresholdHoursSnapshot = cancellationThresholdHoursSnapshot;
        this.cancellationPercentageBeforeThresholdSnapshot =
                cancellationPercentageBeforeThresholdSnapshot;
        this.cancellationPercentageWithinThresholdSnapshot =
                cancellationPercentageWithinThresholdSnapshot;
        this.cancellationAllowedAfterShowStartsSnapshot =
                cancellationAllowedAfterShowStartsSnapshot;
        this.status = status;
        this.paymentStatus = paymentStatus;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public String getBookingReference() { return bookingReference; }
    public Long getShowId() { return showId; }
    public String getMovieTitle() { return movieTitle; }
    public String getTheatreName() { return theatreName; }
    public String getScreenName() { return screenName; }
    public LocalDateTime getShowStartAt() { return showStartAt; }
    public LocalDateTime getShowEndAt() { return showEndAt; }
    public BigDecimal getTicketPrice() { return ticketPrice; }
    public String getCustomerName() { return customerName; }
    public String getCustomerEmail() { return customerEmail; }
    public List<String> getSeats() { return seats; }
    public BigDecimal getTicketSubtotal() { return ticketSubtotal; }
    public BigDecimal getConvenienceFeeAmount() { return convenienceFeeAmount; }
    public BigDecimal getGstOnTicketAmount() { return gstOnTicketAmount; }
    public BigDecimal getGstOnConvenienceFeeAmount() { return gstOnConvenienceFeeAmount; }
    public BigDecimal getGstAmount() { return gstAmount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getFeeTypeSnapshot() { return feeTypeSnapshot; }
    public BigDecimal getConvenienceFeeUnitSnapshot() { return convenienceFeeUnitSnapshot; }
    public Boolean getGstEnabledSnapshot() { return gstEnabledSnapshot; }
    public BigDecimal getGstPercentageSnapshot() { return gstPercentageSnapshot; }
    public Boolean getGstOnTicketPriceSnapshot() { return gstOnTicketPriceSnapshot; }
    public Boolean getGstOnConvenienceFeeSnapshot() { return gstOnConvenienceFeeSnapshot; }
    public Integer getCancellationThresholdHoursSnapshot() { return cancellationThresholdHoursSnapshot; }
    public BigDecimal getCancellationPercentageBeforeThresholdSnapshot() {
        return cancellationPercentageBeforeThresholdSnapshot;
    }
    public BigDecimal getCancellationPercentageWithinThresholdSnapshot() {
        return cancellationPercentageWithinThresholdSnapshot;
    }
    public Boolean getCancellationAllowedAfterShowStartsSnapshot() {
        return cancellationAllowedAfterShowStartsSnapshot;
    }
    public BookingStatus getStatus() { return status; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
