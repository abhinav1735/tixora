
package com.tixora.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ShowRequest {

    @NotNull(message = "Movie ID is required")
    @Positive(message = "Movie ID must be positive")
    private Long movieId;

    @NotNull(message = "Screen ID is required")
    @Positive(message = "Screen ID must be positive")
    private Long screenId;

    @NotNull(message = "Show start time is required")
    @Future(message = "Show start time must be in the future")
    private LocalDateTime startAt;

    @NotNull(message = "Ticket price is required")
    @DecimalMin(value = "0.01", message = "Ticket price must be greater than zero")
    @Digits(integer = 8, fraction = 2,
            message = "Ticket price can have at most 8 integer digits and 2 decimal places")
    private BigDecimal ticketPrice;

    public ShowRequest() {
    }

    public Long getMovieId() {
        return movieId;
    }

    public void setMovieId(Long movieId) {
        this.movieId = movieId;
    }

    public Long getScreenId() {
        return screenId;
    }

    public void setScreenId(Long screenId) {
        this.screenId = screenId;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public void setStartAt(LocalDateTime startAt) {
        this.startAt = startAt;
    }

    public BigDecimal getTicketPrice() {
        return ticketPrice;
    }

    public void setTicketPrice(BigDecimal ticketPrice) {
        this.ticketPrice = ticketPrice;
    }
}
