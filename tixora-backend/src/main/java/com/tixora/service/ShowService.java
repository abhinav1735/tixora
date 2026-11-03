
package com.tixora.service;

import com.tixora.dto.ShowMapper;
import com.tixora.dto.ShowRequest;
import com.tixora.dto.ShowResponse;
import com.tixora.entity.Movie;
import com.tixora.entity.Screen;
import com.tixora.entity.Show;
import com.tixora.exception.ResourceNotFoundException;
import com.tixora.repository.MovieRepository;
import com.tixora.repository.ScreenRepository;
import com.tixora.repository.ShowRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ShowService {

    private static final long SHOW_BUFFER_MINUTES = 15;

    private final ShowRepository showRepository;
    private final MovieRepository movieRepository;
    private final ScreenRepository screenRepository;

    public ShowService(
            ShowRepository showRepository,
            MovieRepository movieRepository,
            ScreenRepository screenRepository
    ) {
        this.showRepository = showRepository;
        this.movieRepository = movieRepository;
        this.screenRepository = screenRepository;
    }

    @Transactional
    public ShowResponse createShow(ShowRequest request) {

        Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Movie with ID " + request.getMovieId() + " does not exist."
                ));

        Screen screen = screenRepository.findById(request.getScreenId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Screen with ID " + request.getScreenId() + " does not exist."
                ));

        LocalDateTime startAt = request.getStartAt();

        if (movie.getDurationMinutes() == null
                || movie.getDurationMinutes() <= 0) {
            throw new IllegalStateException(
                    "Movie duration must be configured before scheduling a show."
            );
        }

        LocalDateTime endAt = startAt.plusMinutes(
                movie.getDurationMinutes()
        );

        // Expand the proposed time range by 15 minutes
        // to enforce the buffer before and after the show.
        LocalDateTime conflictCheckStart = startAt.minusMinutes(
                SHOW_BUFFER_MINUTES
        );

        LocalDateTime conflictCheckEnd = endAt.plusMinutes(
                SHOW_BUFFER_MINUTES
        );

        boolean hasConflict = showRepository.existsOverlappingShow(
                screen.getId(),
                conflictCheckStart,
                conflictCheckEnd,
                null
        );

        if (hasConflict) {
            throw new IllegalStateException(
                    "This screen already has a show scheduled during this time "
                            + "or does not have the required 15-minute buffer."
            );
        }

        Show show = new Show();
        show.setMovie(movie);
        show.setScreen(screen);
        show.setStartAt(startAt);
        show.setEndAt(endAt);
        show.setTicketPrice(request.getTicketPrice());
        show.setActive(true);

        Show savedShow = showRepository.save(show);

        return ShowMapper.toResponse(savedShow);
    }

    public List<ShowResponse> getShowsByScreen(Long screenId) {

        if (!screenRepository.existsById(screenId)) {
            throw new ResourceNotFoundException(
                    "Screen with ID " + screenId + " does not exist."
            );
        }

        return showRepository
                .findByScreenIdOrderByStartAtAsc(screenId)
                .stream()
                .map(ShowMapper::toResponse)
                .toList();
    }

    public List<ShowResponse> getShowsByMovie(Long movieId) {

        if (!movieRepository.existsById(movieId)) {
            throw new ResourceNotFoundException(
                    "Movie with ID " + movieId + " does not exist."
            );
        }

        return showRepository
                .findByMovieIdOrderByStartAtAsc(movieId)
                .stream()
                .map(ShowMapper::toResponse)
                .toList();
    }
}
