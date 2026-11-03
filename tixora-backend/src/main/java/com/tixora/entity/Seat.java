
package com.tixora.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Entity
@Table(
        name = "seats",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_seat_screen_row_number",
                        columnNames = {"screen_id", "row_label", "seat_number"}
                )
        }
)
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Screen is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "screen_id", nullable = false)
    private Screen screen;

    @NotBlank(message = "Row label is required")
    @Size(max = 5, message = "Row label cannot exceed 5 characters")
    @Pattern(
            regexp = "^[A-Z]+$",
            message = "Row label must contain uppercase letters only"
    )
    @Column(name = "row_label", nullable = false, length = 5)
    private String rowLabel;

    @NotNull(message = "Seat number is required")
    @Positive(message = "Seat number must be positive")
    @Min(value = 1, message = "Seat number must be at least 1")
    @Max(value = 1000, message = "Seat number cannot exceed 1000")
    @Column(name = "seat_number", nullable = false)
    private Integer seatNumber;

    @NotNull(message = "Seat category is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeatCategory category;

    @Column(nullable = false)
    private boolean active = true;

    public Seat() {
    }

    public Long getId() {
        return id;
    }

    public Screen getScreen() {
        return screen;
    }

    public void setScreen(Screen screen) {
        this.screen = screen;
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
