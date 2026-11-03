
package com.tixora.controller;

import com.tixora.dto.ShowRequest;
import com.tixora.dto.ShowResponse;
import com.tixora.service.ShowService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shows")
@CrossOrigin(origins = "*")
public class ShowController {

    private final ShowService showService;

    public ShowController(ShowService showService) {
        this.showService = showService;
    }

    // Create a new show
    @PostMapping
    public ResponseEntity<ShowResponse> createShow(
            @Valid @RequestBody ShowRequest request) {

        ShowResponse response = showService.createShow(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // Get all shows for a screen
    @GetMapping("/screen/{screenId}")
    public ResponseEntity<List<ShowResponse>> getShowsByScreen(
            @PathVariable Long screenId) {

        return ResponseEntity.ok(
                showService.getShowsByScreen(screenId)
        );
    }

    // Get all shows for a movie
    @GetMapping("/movie/{movieId}")
    public ResponseEntity<List<ShowResponse>> getShowsByMovie(
            @PathVariable Long movieId) {

        return ResponseEntity.ok(
                showService.getShowsByMovie(movieId)
        );
    }
}
