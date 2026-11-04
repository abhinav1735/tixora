
package com.tixora.repository;

import com.tixora.entity.BookingSeat;
import com.tixora.entity.BookingStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookingSeatRepository
        extends JpaRepository<BookingSeat, Long> {

    List<BookingSeat> findByBookingId(Long bookingId);

    List<BookingSeat> findByShowId(Long showId);

    @Query("""
        SELECT bs.seat.id
        FROM BookingSeat bs
        WHERE bs.show.id = :showId
          AND bs.booking.status IN :statuses
        """)
    List<Long> findBookedSeatIds(
            @Param("showId") Long showId,
            @Param("statuses") List<BookingStatus> statuses
    );

    boolean existsByShowIdAndSeatId(Long showId, Long seatId);
}
