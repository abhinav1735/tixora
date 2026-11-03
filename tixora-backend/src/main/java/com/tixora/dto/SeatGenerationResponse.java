
package com.tixora.dto;

import java.util.List;

public class SeatGenerationResponse {

    private Long screenId;
    private String screenName;
    private int totalGenerated;
    private List<SeatResponse> seats;

    public SeatGenerationResponse() {
    }

    public SeatGenerationResponse(
            Long screenId,
            String screenName,
            int totalGenerated,
            List<SeatResponse> seats
    ) {
        this.screenId = screenId;
        this.screenName = screenName;
        this.totalGenerated = totalGenerated;
        this.seats = seats;
    }

    public Long getScreenId() {
        return screenId;
    }

    public String getScreenName() {
        return screenName;
    }

    public int getTotalGenerated() {
        return totalGenerated;
    }

    public List<SeatResponse> getSeats() {
        return seats;
    }
}
