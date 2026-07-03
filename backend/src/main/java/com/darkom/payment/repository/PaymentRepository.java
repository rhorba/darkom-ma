package com.darkom.payment.repository;

import com.darkom.payment.entity.Payment;
import com.darkom.payment.entity.PaymentStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

  List<Payment> findAllByLeaseIdAndStatusOrderByDueDateAsc(UUID leaseId, PaymentStatus status);

  Optional<Payment> findByCmiTransactionId(String cmiTransactionId);

  Optional<Payment> findTopByLeaseIdOrderByDueDateDesc(UUID leaseId);

  List<Payment> findAllByStatusAndDueDateLessThanEqualAndReminderSentAtIsNull(
      PaymentStatus status, LocalDate dueDate);
}
