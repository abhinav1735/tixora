
package com.tixora.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "seat_reservations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_reservation_show_seat",
                        columnNames = {"show_id", "seat_id"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_reservation_booking",
                        columnList = "booking_id"
                ),
                @Index(
                        name = "idx_reservation_expiry",
                        columnList = "expires_at"
                )
        }
)
public class SeatReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "show_id", nullable = false)
    private Show show;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "reserved_at", nullable = false, updatable = false)
    private LocalDateTime reservedAt;

    @PrePersist
    protected void onCreate() {
        if (reservedAt == null) {
            reservedAt = LocalDateTime.now();
        }
    }

    public SeatReservation() {
    }

    public Long getId() {
        return id;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public Show getShow() {
        return show;
    }

    public void setShow(Show show) {
        this.show = show;
    }

    public Seat getSeat() {
        return seat;
    }

    public void setSeat(Seat seat) {
        this.seat = seat;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getReservedAt() {
        return reservedAt;
    }
}
