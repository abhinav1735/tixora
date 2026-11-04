
package com.tixora.controller.admin;

import com.tixora.dto.admin.PricingSettingsRequest;
import com.tixora.dto.admin.PricingSettingsResponse;
import com.tixora.service.PricingSettingsService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/pricing-settings")
public class PricingSettingsController {

    private final PricingSettingsService pricingSettingsService;

    public PricingSettingsController(
            PricingSettingsService pricingSettingsService
    ) {
        this.pricingSettingsService = pricingSettingsService;
    }

    // Get current pricing configuration
    @GetMapping
    public ResponseEntity<PricingSettingsResponse> getSettings() {
        return ResponseEntity.ok(
                pricingSettingsService.getSettings()
        );
    }

    // Create or update pricing configuration
    @PutMapping
    public ResponseEntity<PricingSettingsResponse> updateSettings(
            @Valid @RequestBody PricingSettingsRequest request
    ) {
        return ResponseEntity.ok(
                pricingSettingsService.updateSettings(request)
        );
    }
}
