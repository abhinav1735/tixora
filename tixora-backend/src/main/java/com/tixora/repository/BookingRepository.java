
package com.tixora.repository;

import com.tixora.entity.Booking;
import com.tixora.entity.BookingStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

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

    List<Booking> findByStatusAndExpiresAtLessThanEqual(
            BookingStatus status,
            LocalDateTime currentTime
    );
}
