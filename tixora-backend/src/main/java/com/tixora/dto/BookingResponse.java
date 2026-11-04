
package com.tixora.dto;

import com.tixora.entity.BookingStatus;

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

    private String customerName;
    private String customerEmail;

    private List<String> seats;

    private BigDecimal totalAmount;
    private BookingStatus status;

    private LocalDateTime createdAt;

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
        this.id = id;
        this.bookingReference = bookingReference;
        this.showId = showId;
        this.movieTitle = movieTitle;
        this.theatreName = theatreName;
        this.screenName = screenName;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.seats = seats;
        this.totalAmount = totalAmount;
        this.status = status;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
