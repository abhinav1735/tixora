
package com.tixora.repository;

import com.tixora.entity.SeatReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SeatReservationRepository
        extends JpaRepository<SeatReservation, Long> {

    boolean existsByShowIdAndSeatId(Long showId, Long seatId);

    List<SeatReservation> findByBookingId(Long bookingId);

    List<SeatReservation> findByBookingIdIn(List<Long> bookingIds);

    @Query("""
            SELECT sr.seat.id
            FROM SeatReservation sr
            WHERE sr.show.id = :showId
            """)
    List<Long> findReservedSeatIdsByShowId(
            @Param("showId") Long showId
    );

    @Query("""
            SELECT sr
            FROM SeatReservation sr
            JOIN FETCH sr.booking b
            WHERE b.status = com.tixora.entity.BookingStatus.PENDING
              AND sr.expiresAt <= :now
            """)
    List<SeatReservation> findExpiredReservations(
            @Param("now") LocalDateTime now
    );

    void deleteByBookingId(Long bookingId);

    void deleteByBookingIdIn(List<Long> bookingIds);

    // Preserve seats after successful payment.
    @Modifying
    @Query("""
            UPDATE SeatReservation sr
            SET sr.expiresAt = NULL
            WHERE sr.booking.id = :bookingId
            """)
    int clearExpiryByBookingId(
            @Param("bookingId") Long bookingId
    );
}
