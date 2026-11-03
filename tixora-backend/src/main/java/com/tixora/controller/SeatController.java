
package com.tixora.controller;

import com.tixora.dto.SeatGenerationRequest;
import com.tixora.dto.SeatGenerationResponse;
import com.tixora.service.SeatService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.tixora.dto.SeatResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/seats")
@CrossOrigin(origins = "*")
public class SeatController {

    private final SeatService seatService;

    public SeatController(SeatService seatService) {
        this.seatService = seatService;
    }

    @PostMapping("/generate")
    public ResponseEntity<SeatGenerationResponse> generateSeats(
            @Valid @RequestBody SeatGenerationRequest request
    ) {

        SeatGenerationResponse response =
                seatService.generateSeats(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/screen/{screenId}")
    public ResponseEntity<List<SeatResponse>> getSeatsByScreen(
            @PathVariable Long screenId) {

        return ResponseEntity.ok(seatService.getSeatsByScreen(screenId));
    }

}
