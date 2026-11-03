
package com.tixora.dto;

import com.tixora.entity.SeatCategory;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class SeatRowRequest {

    @NotNull(message = "Row label is required")
    @Size(min = 1, max = 5, message = "Row label must contain 1 to 5 characters")
    @Pattern(
            regexp = "^[A-Z]+$",
            message = "Row label must contain uppercase letters only"
    )
    private String rowLabel;

    @NotNull(message = "Seat count is required")
    @Min(value = 1, message = "Each row must have at least one seat")
    @Max(value = 100, message = "A row cannot have more than 100 seats")
    private Integer seatCount;

    @NotNull(message = "Seat category is required")
    private SeatCategory category;

    public SeatRowRequest() {
    }

    public String getRowLabel() {
        return rowLabel;
    }

    public void setRowLabel(String rowLabel) {
        this.rowLabel = rowLabel;
    }

    public Integer getSeatCount() {
        return seatCount;
    }

    public void setSeatCount(Integer seatCount) {
        this.seatCount = seatCount;
    }

    public SeatCategory getCategory() {
        return category;
    }

    public void setCategory(SeatCategory category) {
        this.category = category;
    }
}
