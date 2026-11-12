
package com.tixora.controller;

import com.tixora.service.RazorpaySignatureService;
import com.tixora.service.RazorpayWebhookService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class RazorpayWebhookController {

    private final RazorpaySignatureService signatureService;
    private final RazorpayWebhookService webhookService;

    public RazorpayWebhookController(
            RazorpaySignatureService signatureService,
            RazorpayWebhookService webhookService
    ) {
        this.signatureService = signatureService;
        this.webhookService = webhookService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> receive(
            @RequestHeader(
                    value = "X-Razorpay-Signature",
                    required = false
            ) String signature,

            @RequestHeader(
                    value = "X-Razorpay-Event-Id",
                    required = false
            ) String eventId,

            @RequestBody byte[] rawBody
    ) {

        // Verify Razorpay webhook signature before processing.
        if (!signatureService.verifyWebhookSignature(rawBody, signature)) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .build();
        }

        // Process the verified webhook event.
        webhookService.process(eventId, rawBody);

        return ResponseEntity.ok().build();
    }
}
