
package com.tixora.dto;

import com.tixora.entity.Show;

public class ShowMapper {

    private ShowMapper() {
        // Utility class
    }

    public static ShowResponse toResponse(Show show) {

        if (show == null) {
            return null;
        }

        if (show.getMovie() == null || show.getScreen() == null
                || show.getScreen().getTheatre() == null) {
            throw new IllegalStateException(
                    "Show must have a movie, screen, and theatre."
            );
        }

        return new ShowResponse(
                show.getId(),

                show.getMovie().getId(),
                show.getMovie().getTitle(),
                show.getMovie().getDurationMinutes(),

                show.getScreen().getTheatre().getId(),
                show.getScreen().getTheatre().getName(),
                show.getScreen().getTheatre().getCity(),

                show.getScreen().getId(),
                show.getScreen().getName(),

                show.getStartAt(),
                show.getEndAt(),

                show.getTicketPrice(),
                show.isActive()
        );
    }
}
