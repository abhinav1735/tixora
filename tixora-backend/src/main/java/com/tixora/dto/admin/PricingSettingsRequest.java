
package com.tixora.dto.admin;

import com.tixora.entity.PricingSettings;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PricingSettingsRequest(

        @NotNull(message = "Convenience fee is required")
        @DecimalMin(value = "0.00", message = "Convenience fee cannot be negative")
        @Digits(integer = 8, fraction = 2)
        BigDecimal convenienceFee,

        @NotNull(message = "Fee type is required")
        PricingSettings.FeeType feeType,

        @NotNull(message = "GST enabled setting is required")
        Boolean gstEnabled,

        @NotNull(message = "GST percentage is required")
        @DecimalMin("0.00")
        @DecimalMax("100.00")
        @Digits(integer = 3, fraction = 2)
        BigDecimal gstPercentage,

        @NotNull(message = "GST applicability on ticket price is required")
        Boolean gstOnTicketPrice,

        @NotNull(message = "GST applicability on convenience fee is required")
        Boolean gstOnConvenienceFee,

        @NotNull(message = "Cancellation threshold is required")
        @Min(value = 0, message = "Cancellation threshold cannot be negative")
        Integer cancellationThresholdHours,

        @NotNull(message = "Cancellation percentage before threshold is required")
        @DecimalMin("0.00")
        @DecimalMax("100.00")
        @Digits(integer = 3, fraction = 2)
        BigDecimal cancellationPercentageBeforeThreshold,

        @NotNull(message = "Cancellation percentage within threshold is required")
        @DecimalMin("0.00")
        @DecimalMax("100.00")
        @Digits(integer = 3, fraction = 2)
        BigDecimal cancellationPercentageWithinThreshold

) {
}
