
package com.tixora.repository;

import com.tixora.entity.Seat;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    @Override
    @EntityGraph(attributePaths = "screen")
    List<Seat> findAll();

    @Override
    @EntityGraph(attributePaths = "screen")
    Optional<Seat> findById(Long id);

    List<Seat> findByScreenIdOrderByRowLabelAscSeatNumberAsc(Long screenId);

    boolean existsByScreenIdAndRowLabelAndSeatNumber(
            Long screenId,
            String rowLabel,
            Integer seatNumber
    );

    long countByScreenId(Long screenId);

    long countByScreenIdAndActiveTrue(Long screenId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT s
        FROM Seat s
        JOIN FETCH s.screen
        WHERE s.id IN :seatIds
        ORDER BY s.id
        """)
    List<Seat> findAllByIdInForUpdate(
            @Param("seatIds") List<Long> seatIds
    );
}
