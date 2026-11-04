
package com.tixora.service;

import com.tixora.dto.admin.PricingSettingsRequest;
import com.tixora.dto.admin.PricingSettingsResponse;
import com.tixora.entity.PricingSettings;
import com.tixora.repository.PricingSettingsRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class PricingSettingsService {

    private static final Long SETTINGS_ID = 1L;

    private final PricingSettingsRepository pricingSettingsRepository;

    public PricingSettingsService(
            PricingSettingsRepository pricingSettingsRepository
    ) {
        this.pricingSettingsRepository = pricingSettingsRepository;
    }

    public PricingSettingsResponse getSettings() {
        PricingSettings settings = getExistingSettings();
        return toResponse(settings);
    }

    @Transactional
    public PricingSettingsResponse updateSettings(
            PricingSettingsRequest request
    ) {
        validateRequest(request);

        PricingSettings settings = pricingSettingsRepository
                .findById(SETTINGS_ID)
                .orElseGet(PricingSettings::new);

        settings.setConvenienceFee(request.convenienceFee());
        settings.setFeeType(request.feeType());

        settings.setGstEnabled(request.gstEnabled());
        settings.setGstPercentage(request.gstPercentage());
        settings.setGstOnTicketPrice(request.gstOnTicketPrice());
        settings.setGstOnConvenienceFee(
                request.gstOnConvenienceFee()
        );

        settings.setCancellationThresholdHours(
                request.cancellationThresholdHours()
        );

        settings.setCancellationPercentageBeforeThreshold(
                request.cancellationPercentageBeforeThreshold()
        );

        settings.setCancellationPercentageWithinThreshold(
                request.cancellationPercentageWithinThreshold()
        );

        PricingSettings savedSettings =
                pricingSettingsRepository.save(settings);

        return toResponse(savedSettings);
    }

    private PricingSettings getExistingSettings() {
        return pricingSettingsRepository.findById(SETTINGS_ID)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Pricing settings have not been configured yet."
                ));
    }

    private void validateRequest(PricingSettingsRequest request) {
        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Pricing settings request is required."
            );
        }

        if (Boolean.TRUE.equals(request.gstEnabled())
                && !Boolean.TRUE.equals(request.gstOnTicketPrice())
                && !Boolean.TRUE.equals(request.gstOnConvenienceFee())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "GST must apply to at least one charge when enabled."
            );
        }

        if (request.cancellationThresholdHours() == null
                || request.cancellationPercentageBeforeThreshold() == null
                || request.cancellationPercentageWithinThreshold() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Complete cancellation settings are required."
            );
        }
    }

    private PricingSettingsResponse toResponse(
            PricingSettings settings
    ) {
        return new PricingSettingsResponse(
                settings.getConvenienceFee(),
                settings.getFeeType(),
                settings.getGstEnabled(),
                settings.getGstPercentage(),
                settings.getGstOnTicketPrice(),
                settings.getGstOnConvenienceFee(),
                settings.getCancellationThresholdHours(),
                settings.getCancellationPercentageBeforeThreshold(),
                settings.getCancellationPercentageWithinThreshold(),
                settings.isCancellationAllowedAfterShowStarts(),
                settings.getUpdatedAt()
        );
    }
}
