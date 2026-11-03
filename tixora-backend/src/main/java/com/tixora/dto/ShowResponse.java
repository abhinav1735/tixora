
package com.tixora.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ShowResponse {

    private Long id;

    private Long movieId;
    private String movieTitle;
    private Integer movieDurationMinutes;

    private Long theatreId;
    private String theatreName;
    private String city;

    private Long screenId;
    private String screenName;

    private LocalDateTime startAt;
    private LocalDateTime endAt;

    private BigDecimal ticketPrice;

    private boolean active;

    public ShowResponse(
            Long id,
            Long movieId,
            String movieTitle,
            Integer movieDurationMinutes,
            Long theatreId,
            String theatreName,
            String city,
            Long screenId,
            String screenName,
            LocalDateTime startAt,
            LocalDateTime endAt,
            BigDecimal ticketPrice,
            boolean active
    ) {
        this.id = id;
        this.movieId = movieId;
        this.movieTitle = movieTitle;
        this.movieDurationMinutes = movieDurationMinutes;
        this.theatreId = theatreId;
        this.theatreName = theatreName;
        this.city = city;
        this.screenId = screenId;
        this.screenName = screenName;
        this.startAt = startAt;
        this.endAt = endAt;
        this.ticketPrice = ticketPrice;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public Long getMovieId() {
        return movieId;
    }

    public String getMovieTitle() {
        return movieTitle;
    }

    public Integer getMovieDurationMinutes() {
        return movieDurationMinutes;
    }

    public Long getTheatreId() {
        return theatreId;
    }

    public String getTheatreName() {
        return theatreName;
    }

    public String getCity() {
        return city;
    }

    public Long getScreenId() {
        return screenId;
    }

    public String getScreenName() {
        return screenName;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public BigDecimal getTicketPrice() {
        return ticketPrice;
    }

    public boolean isActive() {
        return active;
    }
}
