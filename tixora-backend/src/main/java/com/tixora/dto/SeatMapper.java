
package com.tixora.dto;

import com.tixora.dto.SeatResponse;
import com.tixora.entity.Seat;
import com.tixora.entity.Screen;

public final class SeatMapper {

    private SeatMapper() {
        // Utility class
    }

    public static SeatResponse toResponse(Seat seat) {

        if (seat == null) {
            return null;
        }

        Screen screen = seat.getScreen();

        if (screen == null) {
            throw new IllegalStateException(
                    "Seat must be associated with a screen."
            );
        }

        return new SeatResponse(
                seat.getId(),
                screen.getId(),
                screen.getName(),
                seat.getRowLabel(),
                seat.getSeatNumber(),
                seat.getCategory(),
                seat.isActive()
        );
    }
}
