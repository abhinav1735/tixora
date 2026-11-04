
package com.tixora.repository;

import com.tixora.entity.Payment;
import com.tixora.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    List<Payment> findAllByBookingIdOrderByCreatedAtDesc(Long bookingId);

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

    Optional<Payment> findFirstByBookingIdOrderByCreatedAtDesc(Long bookingId);

    @EntityGraph(attributePaths = {"booking"})
    Optional<Payment> findWithBookingByRazorpayOrderId(String razorpayOrderId);

    Optional<Payment> findByReceipt(String receipt);
}
