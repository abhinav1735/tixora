package com.tixora.repository;

import com.tixora.entity.RazorpayWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RazorpayWebhookEventRepository
        extends JpaRepository<RazorpayWebhookEvent, Long> {

    boolean existsByEventId(String eventId);
}
