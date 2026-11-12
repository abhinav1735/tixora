package com.tixora.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.tixora.entity.RazorpayWebhookEvent;
import com.tixora.repository.RazorpayWebhookEventRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RazorpayWebhookService {

    private final ObjectMapper objectMapper;
    private final RazorpayWebhookEventRepository eventRepository;
    private final PaymentService paymentService;
    private final PaymentTransactionService paymentTransactionService;

    public RazorpayWebhookService(
            ObjectMapper objectMapper,
            RazorpayWebhookEventRepository eventRepository,
            PaymentService paymentService,
            PaymentTransactionService paymentTransactionService
    ) {
        this.objectMapper = objectMapper;
        this.eventRepository = eventRepository;
        this.paymentService = paymentService;
        this.paymentTransactionService = paymentTransactionService;
    }

    @Transactional
    public void process(String eventId, byte[] rawBody) {
        if (eventId == null || eventId.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Missing Razorpay event ID");
        }

        final JsonNode root;
        try {
            root = objectMapper.readTree(rawBody);
        } catch (Exception exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid webhook JSON", exception);
        }

        String eventType = root.path("event").asText(null);
        if (eventType == null || eventType.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Missing webhook event type");
        }

        if (eventRepository.existsByEventId(eventId)) {
            return;
        }

        eventRepository.saveAndFlush(
                new RazorpayWebhookEvent(eventId, eventType));

        JsonNode payload = root.path("payload");

        switch (eventType) {
            case "payment.captured" -> processCaptured(payload);
            case "refund.created", "refund.processed", "refund.failed" ->
                    processRefund(payload);
            case "payment.failed" -> {
                // A failed attempt is not allowed to overwrite a captured payment.
                // Existing checkout failure handling remains authoritative.
            }
            default -> {
                // Unknown events are recorded and acknowledged to avoid endless retries.
            }
        }
    }

    private void processCaptured(JsonNode payload) {
        JsonNode entity = payload.path("payment").path("entity");
        String paymentId = text(entity, "id");
        String orderId = text(entity, "order_id");
        Long amount = longValue(entity, "amount");
        String currency = text(entity, "currency");
        String status = text(entity, "status");

        if (paymentId == null || orderId == null || amount == null
                || !"captured".equalsIgnoreCase(status)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid payment.captured payload");
        }

        paymentService.handleCapturedWebhook(orderId, paymentId, amount, currency);
    }

    private void processRefund(JsonNode payload) {
        JsonNode entity = payload.path("refund").path("entity");
        String refundId = text(entity, "id");
        String paymentId = text(entity, "payment_id");
        Long amount = longValue(entity, "amount");
        String status = text(entity, "status");

        if (refundId == null || paymentId == null || amount == null || status == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid refund webhook payload");
        }

        paymentTransactionService.handleRefundWebhook(
                paymentId, refundId, amount, status);
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private Long longValue(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() || !value.canConvertToLong()
                ? null : value.asLong();
    }
}
