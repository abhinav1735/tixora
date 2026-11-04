
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

    private BigDecimal totalAmount;
    private BookingStatus status;
    private PaymentStatus paymentStatus;

    private LocalDateTime createdAt;

    // Existing constructor — preserves current booking creation flow
    public BookingResponse(
            Long id,
            String bookingReference,
            Long showId,
            String movieTitle,
            String theatreName,
            String screenName,
            String customerName,
            String customerEmail,
            List<String> seats,
            BigDecimal totalAmount,
            BookingStatus status,
            LocalDateTime createdAt
    ) {
        this(
                id, bookingReference, showId,
                movieTitle, theatreName, screenName,
                null, null, null,
                customerName, customerEmail, seats,
                totalAmount, status, null, createdAt
        );
    }

    // Extended constructor
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
            BigDecimal totalAmount,
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
        this.totalAmount = totalAmount;
        this.status = status;
        this.paymentStatus = paymentStatus;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getBookingReference() {
        return bookingReference;
    }

    public Long getShowId() {
        return showId;
    }

    public String getMovieTitle() {
        return movieTitle;
    }

    public String getTheatreName() {
        return theatreName;
    }

    public String getScreenName() {
        return screenName;
    }

    public LocalDateTime getShowStartAt() {
        return showStartAt;
    }

    public LocalDateTime getShowEndAt() {
        return showEndAt;
    }

    public BigDecimal getTicketPrice() {
        return ticketPrice;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public List<String> getSeats() {
        return seats;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
