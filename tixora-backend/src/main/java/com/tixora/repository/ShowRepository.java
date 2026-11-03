
package com.tixora.repository;

import com.tixora.entity.Show;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ShowRepository extends JpaRepository<Show, Long> {

    @EntityGraph(attributePaths = {
            "movie",
            "screen",
            "screen.theatre"
    })
    List<Show> findByScreenIdOrderByStartAtAsc(Long screenId);


    @EntityGraph(attributePaths = {
            "movie",
            "screen",
            "screen.theatre"
    })
    List<Show> findByMovieIdOrderByStartAtAsc(Long movieId);


    @Query("""
        SELECT COUNT(s) > 0
        FROM Show s
        WHERE s.screen.id = :screenId
          AND s.active = true
          AND (:excludeShowId IS NULL OR s.id <> :excludeShowId)
          AND s.startAt < :newEndAt
          AND s.endAt > :newStartAt
        """)
    boolean existsOverlappingShow(
            @Param("screenId") Long screenId,
            @Param("newStartAt") LocalDateTime newStartAt,
            @Param("newEndAt") LocalDateTime newEndAt,
            @Param("excludeShowId") Long excludeShowId
    );
}
