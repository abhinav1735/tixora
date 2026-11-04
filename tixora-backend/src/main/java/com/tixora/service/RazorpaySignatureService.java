package com.tixora.service;

import com.tixora.config.RazorpayProperties;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.GeneralSecurityException;
import java.util.HexFormat;

@Service
public class RazorpaySignatureService {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private final RazorpayProperties razorpayProperties;

    public RazorpaySignatureService(
            RazorpayProperties razorpayProperties
    ) {
        this.razorpayProperties = razorpayProperties;
    }

    public boolean verifySignature(
            String orderId,
            String paymentId,
            String receivedSignature
    ) {

        if (orderId == null || paymentId == null
                || receivedSignature == null
                || !receivedSignature.matches("[a-fA-F0-9]{64}")) {
            return false;
        }

        try {
            String payload = orderId + "|" + paymentId;

            Mac mac = Mac.getInstance(HMAC_SHA256);

            SecretKeySpec secretKey = new SecretKeySpec(
                    razorpayProperties.keySecret()
                            .getBytes(StandardCharsets.UTF_8),
                    HMAC_SHA256
            );

            mac.init(secretKey);

            byte[] hash = mac.doFinal(
                    payload.getBytes(StandardCharsets.UTF_8)
            );

            String expectedSignature = HexFormat.of().formatHex(hash);

            return MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.US_ASCII),
                    receivedSignature.toLowerCase()
                            .getBytes(StandardCharsets.US_ASCII)
            );

        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(
                    "Unable to verify Razorpay payment signature",
                    exception
            );
        }
    }
}