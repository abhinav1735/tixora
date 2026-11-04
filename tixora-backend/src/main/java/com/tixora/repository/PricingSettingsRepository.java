
package com.tixora.repository;

import com.tixora.entity.PricingSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PricingSettingsRepository
        extends JpaRepository<PricingSettings, Long> {
}
