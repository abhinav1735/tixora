
package com.tixora.repository;

import com.tixora.entity.Payment;
import com.tixora.entity.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    @EntityGraph(attributePaths = {"booking"})
    Optional<Payment> findWithBookingById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT p
            FROM Payment p
            WHERE p.razorpayPaymentId = :paymentId
            """)
    Optional<Payment> findByRazorpayPaymentIdForUpdate(
            @Param("paymentId") String paymentId
    );

    List<Payment> findAllByBookingIdOrderByCreatedAtDesc(
            Long bookingId
    );

    boolean existsByRazorpayOrderId(String razorpayOrderId);

    Optional<Payment> findFirstByBookingIdAndStatusOrderByCreatedAtDesc(
            Long bookingId,
            PaymentStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT p
            FROM Payment p
            WHERE p.razorpayOrderId = :orderId
            """)
    Optional<Payment> findByRazorpayOrderIdForUpdate(
            @Param("orderId") String orderId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT p
            FROM Payment p
            WHERE p.id = :paymentId
            """)
    Optional<Payment> findByIdForUpdate(
            @Param("paymentId") Long paymentId
    );

    Optional<Payment> findFirstByBookingIdOrderByCreatedAtDesc(
            Long bookingId
    );

    @EntityGraph(attributePaths = {"booking"})
    Optional<Payment> findWithBookingByRazorpayOrderId(
            String razorpayOrderId
    );

    Optional<Payment> findByReceipt(String receipt);

    List<Payment> findAllByStatusOrderByRefundInitiatedAtAsc(
            PaymentStatus status
    );
}
