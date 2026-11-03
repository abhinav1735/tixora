
package com.tixora.dto;

import com.tixora.entity.SeatCategory;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class SeatRequest {

    @NotNull(message = "Screen ID is required")
    @Positive(message = "Screen ID must be positive")
    private Long screenId;

    @Pattern(
            regexp = "^[A-Z]+$",
            message = "Row label must contain uppercase letters only"
    )
    @Size(max = 5, message = "Row label cannot exceed 5 characters")
    private String rowLabel;

    @Min(value = 1, message = "Seat number must be at least 1")
    @Max(value = 1000, message = "Seat number cannot exceed 1000")
    private Integer seatNumber;

    @NotNull(message = "Seat category is required")
    private SeatCategory category;

    public SeatRequest() {
    }

    public Long getScreenId() {
        return screenId;
    }

    public void setScreenId(Long screenId) {
        this.screenId = screenId;
    }

    public String getRowLabel() {
        return rowLabel;
    }

    public void setRowLabel(String rowLabel) {
        this.rowLabel = rowLabel;
    }

    public Integer getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(Integer seatNumber) {
        this.seatNumber = seatNumber;
    }

    public SeatCategory getCategory() {
        return category;
    }

    public void setCategory(SeatCategory category) {
        this.category = category;
    }
}
