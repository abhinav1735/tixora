
package com.tixora.service;

import com.tixora.dto.SeatGenerationRequest;
import com.tixora.dto.SeatGenerationResponse;
import com.tixora.dto.SeatMapper;
import com.tixora.dto.SeatResponse;
import com.tixora.dto.SeatRowRequest;
import com.tixora.entity.Screen;
import com.tixora.entity.Seat;
import com.tixora.repository.ScreenRepository;
import com.tixora.repository.SeatRepository;
import com.tixora.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class SeatService {

    private final SeatRepository seatRepository;
    private final ScreenRepository screenRepository;

    public SeatService(
            SeatRepository seatRepository,
            ScreenRepository screenRepository
    ) {
        this.seatRepository = seatRepository;
        this.screenRepository = screenRepository;
    }

    @Transactional
    public SeatGenerationResponse generateSeats(
            SeatGenerationRequest request
    ) {

        // 1. Verify that the screen exists.
        Screen screen = screenRepository.findById(request.getScreenId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Screen with ID " + request.getScreenId()
                                + " does not exist."
                ));

        // 2. Prevent generating seats more than once.
        if (seatRepository.countByScreenId(screen.getId()) > 0) {
            throw new IllegalStateException(
                    "Seats have already been generated for screen ID "
                            + screen.getId() + "."
            );
        }

        // 3. Validate row labels and calculate total seats.
        Set<String> rowLabels = new HashSet<>();
        int totalSeatsToGenerate = 0;

        for (SeatRowRequest row : request.getRows()) {

            String rowLabel = row.getRowLabel();

            if (!rowLabels.add(rowLabel)) {
                throw new IllegalArgumentException(
                        "Duplicate row label: " + rowLabel
                );
            }

            totalSeatsToGenerate += row.getSeatCount();
        }

        // 4. Ensure generated capacity matches screen capacity.
        if (totalSeatsToGenerate != screen.getTotalSeats()) {
            throw new IllegalArgumentException(
                    "Configured screen capacity is "
                            + screen.getTotalSeats()
                            + ", but the requested layout contains "
                            + totalSeatsToGenerate + " seats."
            );
        }

        // 5. Generate individual seat entities.
        List<Seat> seatsToSave = new ArrayList<>();

        for (SeatRowRequest row : request.getRows()) {

            for (int seatNumber = 1;
                 seatNumber <= row.getSeatCount();
                 seatNumber++) {

                Seat seat = new Seat();

                seat.setScreen(screen);
                seat.setRowLabel(row.getRowLabel());
                seat.setSeatNumber(seatNumber);
                seat.setCategory(row.getCategory());

                seatsToSave.add(seat);
            }
        }

        // 6. Save all seats within the current transaction.
        List<Seat> savedSeats = seatRepository.saveAll(seatsToSave);

        // 7. Convert entities into response DTOs.
        List<SeatResponse> responses = savedSeats.stream()
                .map(SeatMapper::toResponse)
                .toList();

        return new SeatGenerationResponse(
                screen.getId(),
                screen.getName(),
                responses.size(),
                responses
        );
    }


    @Transactional(readOnly = true)
    public List<SeatResponse> getSeatsByScreen(Long screenId) {

        if (!screenRepository.existsById(screenId)) {
            throw new ResourceNotFoundException(
                    "Screen with ID " + screenId + " does not exist."
            );
        }

        return seatRepository
                .findByScreenIdOrderByRowLabelAscSeatNumberAsc(screenId)
                .stream()
                .map(SeatMapper::toResponse)
                .toList();
    }

}
