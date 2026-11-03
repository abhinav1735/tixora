
package com.tixora.repository;

import com.tixora.entity.Seat;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    @Override
    @EntityGraph(attributePaths = "screen")
    List<Seat> findAll();

    @Override
    @EntityGraph(attributePaths = "screen")
    Optional<Seat> findById(Long id);

    @EntityGraph(attributePaths = "screen")
    List<Seat> findByScreenIdOrderByRowLabelAscSeatNumberAsc(Long screenId);

    boolean existsByScreenIdAndRowLabelAndSeatNumber(
            Long screenId,
            String rowLabel,
            Integer seatNumber
    );

    long countByScreenId(Long screenId);

    long countByScreenIdAndActiveTrue(Long screenId);
}
