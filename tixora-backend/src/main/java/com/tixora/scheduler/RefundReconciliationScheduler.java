
package com.tixora.scheduler;

import com.tixora.entity.Payment;
import com.tixora.entity.PaymentStatus;
import com.tixora.repository.PaymentRepository;
import com.tixora.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RefundReconciliationScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(RefundReconciliationScheduler.class);

    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;

    public RefundReconciliationScheduler(
            PaymentRepository paymentRepository,
            PaymentService paymentService) {
        this.paymentRepository = paymentRepository;
        this.paymentService = paymentService;
    }

    @Scheduled(
            fixedDelayString = "${tixora.refund.reconciliation-interval:60000}"
    )
    public void reconcilePendingRefunds() {

        List<Payment> pendingPayments =
                paymentRepository.findAllByStatusOrderByRefundInitiatedAtAsc(
                        PaymentStatus.REFUND_PENDING
                );

        if (pendingPayments.isEmpty()) {
            return;
        }

        log.info(
                "Starting refund reconciliation for {} pending payments",
                pendingPayments.size()
        );

        for (Payment payment : pendingPayments) {
            try {
                paymentService.reconcileRefund(payment.getId());
            } catch (Exception exception) {
                log.error(
                        "Refund reconciliation failed for payment ID: {}",
                        payment.getId(),
                        exception
                );
            }
        }
    }
}
