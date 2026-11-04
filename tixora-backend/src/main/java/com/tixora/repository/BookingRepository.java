
package com.tixora.repository;

import com.tixora.entity.Booking;
import com.tixora.entity.BookingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByBookingReference(String bookingReference);

    boolean existsByBookingReference(String bookingReference);

    @EntityGraph(attributePaths = {
            "show",
            "show.movie",
            "show.screen",
            "show.screen.theatre"
    })
    Optional<Booking> findWithDetailsByBookingReference(
            String bookingReference
    );

    // Find expired bookings while locking their rows.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT b
            FROM Booking b
            WHERE b.status = :status
              AND b.expiresAt <= :currentTime
            ORDER BY b.id
            """)
    List<Booking> findExpiredBookingsForUpdate(
            @Param("status") BookingStatus status,
            @Param("currentTime") LocalDateTime currentTime
    );

    // Find an individual booking with a write lock.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT b
            FROM Booking b
            WHERE b.bookingReference = :bookingReference
            """)
    Optional<Booking> findByBookingReferenceForUpdate(
            @Param("bookingReference") String bookingReference
    );
}
