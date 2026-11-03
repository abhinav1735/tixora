
package com.tixora.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public class SeatGenerationRequest {

    @NotNull(message = "Screen ID is required")
    @Positive(message = "Screen ID must be positive")
    private Long screenId;

    @NotEmpty(message = "At least one row must be provided")
    @Size(max = 100, message = "A screen cannot have more than 100 rows")
    private List<@NotNull(message = "Row configuration cannot be null")
    @Valid SeatRowRequest> rows;

    public SeatGenerationRequest() {
    }

    public Long getScreenId() {
        return screenId;
    }

    public void setScreenId(Long screenId) {
        this.screenId = screenId;
    }

    public List<SeatRowRequest> getRows() {
        return rows;
    }

    public void setRows(List<SeatRowRequest> rows) {
        this.rows = rows;
    }
}
