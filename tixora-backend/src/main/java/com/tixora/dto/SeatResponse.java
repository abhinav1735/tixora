
package com.tixora.dto;

import com.tixora.entity.SeatCategory;

public class SeatResponse {

    private Long id;
    private Long screenId;
    private String screenName;
    private String rowLabel;
    private Integer seatNumber;
    private String seatLabel;
    private SeatCategory category;
    private boolean active;

    public SeatResponse() {
    }

    public SeatResponse(
            Long id,
            Long screenId,
            String screenName,
            String rowLabel,
            Integer seatNumber,
            SeatCategory category,
            boolean active
    ) {
        this.id = id;
        this.screenId = screenId;
        this.screenName = screenName;
        this.rowLabel = rowLabel;
        this.seatNumber = seatNumber;
        this.seatLabel = rowLabel + seatNumber;
        this.category = category;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public Long getScreenId() {
        return screenId;
    }

    public String getScreenName() {
        return screenName;
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

    public boolean isActive() {
        return active;
    }
}
