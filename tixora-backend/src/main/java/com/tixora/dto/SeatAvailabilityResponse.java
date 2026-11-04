
package com.tixora.dto;

import com.tixora.entity.SeatCategory;

public class SeatAvailabilityResponse {

    private Long seatId;
    private String rowLabel;
    private Integer seatNumber;
    private String seatLabel;
    private SeatCategory category;
    private boolean available;

    public SeatAvailabilityResponse(
            Long seatId,
            String rowLabel,
            Integer seatNumber,
            String seatLabel,
            SeatCategory category,
            boolean available
    ) {
        this.seatId = seatId;
        this.rowLabel = rowLabel;
        this.seatNumber = seatNumber;
        this.seatLabel = seatLabel;
        this.category = category;
        this.available = available;
    }

    public Long getSeatId() {
        return seatId;
    }

    public String getRowLabel() {
        return rowLabel;
    }

    public Integer getSeatNumber() {
        return seatNumber;
    }

    public String getSeatLabel() {
        return seatLabel;
    }

    public SeatCategory getCategory() {
        return category;
    }

    public boolean isAvailable() {
        return available;
    }
}
