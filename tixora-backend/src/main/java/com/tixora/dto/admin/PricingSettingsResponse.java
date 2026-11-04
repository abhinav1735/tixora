
package com.tixora.dto.admin;

import com.tixora.entity.PricingSettings;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PricingSettingsResponse(

        BigDecimal convenienceFee,

        PricingSettings.FeeType feeType,

        Boolean gstEnabled,

        BigDecimal gstPercentage,

        Boolean gstOnTicketPrice,

        Boolean gstOnConvenienceFee,

        Integer cancellationThresholdHours,

        BigDecimal cancellationPercentageBeforeThreshold,

        BigDecimal cancellationPercentageWithinThreshold,

        boolean cancellationAllowedAfterShowStarts,

        LocalDateTime updatedAt

) {
}
