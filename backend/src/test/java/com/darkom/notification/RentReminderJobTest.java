package com.darkom.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.darkom.auth.entity.Role;
import com.darkom.auth.entity.User;
import com.darkom.auth.repository.UserRepository;
import com.darkom.lease.entity.Lease;
import com.darkom.lease.entity.LeaseStatus;
import com.darkom.lease.repository.LeaseRepository;
import com.darkom.payment.entity.Payment;
import com.darkom.payment.entity.PaymentStatus;
import com.darkom.payment.repository.PaymentRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RentReminderJobTest {

  @Mock private LeaseRepository leaseRepository;
  @Mock private PaymentRepository paymentRepository;
  @Mock private UserRepository userRepository;
  @Mock private EmailSender emailSender;

  private final Clock clock = Clock.fixed(Instant.parse("2026-03-01T00:00:00Z"), ZoneOffset.UTC);
  private RentReminderJob job;

  @BeforeEach
  void setUp() {
    RentReminderProperties properties = new RentReminderProperties();
    properties.setDaysBeforeDue(3);
    properties.setCron("0 0 8 * * *");
    job =
        new RentReminderJob(
            leaseRepository, paymentRepository, userRepository, emailSender, properties, clock);
  }

  private Lease activeLease(UUID id, UUID tenantId, LocalDate endDate) {
    Lease lease = new Lease();
    lease.setId(id);
    lease.setTenantId(tenantId);
    lease.setStatus(LeaseStatus.ACTIVE);
    lease.setMonthlyRent(new BigDecimal("3500.00"));
    lease.setEndDate(endDate);
    return lease;
  }

  private Payment payment(UUID leaseId, LocalDate dueDate, PaymentStatus status) {
    Payment payment = new Payment();
    payment.setId(UUID.randomUUID());
    payment.setLeaseId(leaseId);
    payment.setAmount(new BigDecimal("3500.00"));
    payment.setDueDate(dueDate);
    payment.setStatus(status);
    return payment;
  }

  @Test
  void generatesNextPaymentWhenLatestIsWithinTheReminderWindow() {
    UUID leaseId = UUID.randomUUID();
    Lease lease = activeLease(leaseId, UUID.randomUUID(), LocalDate.of(2027, 1, 1));
    Payment latest =
        payment(
            leaseId,
            LocalDate.of(2026, 3, 2),
            PaymentStatus.PAID); // within 3-day horizon of 2026-03-01

    when(leaseRepository.findAllByStatus(LeaseStatus.ACTIVE)).thenReturn(List.of(lease));
    when(paymentRepository.findTopByLeaseIdOrderByDueDateDesc(leaseId))
        .thenReturn(Optional.of(latest));
    when(paymentRepository.findAllByStatusAndDueDateLessThanEqualAndReminderSentAtIsNull(
            any(), any()))
        .thenReturn(List.of());

    job.runDailyJob();

    ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
    verify(paymentRepository).save(captor.capture());
    Payment created = captor.getValue();
    assertThat(created.getLeaseId()).isEqualTo(leaseId);
    assertThat(created.getDueDate()).isEqualTo(LocalDate.of(2026, 4, 2));
    assertThat(created.getStatus()).isEqualTo(PaymentStatus.PENDING);
    assertThat(created.getAmount()).isEqualByComparingTo("3500.00");
  }

  @Test
  void doesNotGenerateWhenLatestPaymentIsFarInTheFuture() {
    UUID leaseId = UUID.randomUUID();
    Lease lease = activeLease(leaseId, UUID.randomUUID(), LocalDate.of(2027, 1, 1));
    Payment latest =
        payment(leaseId, LocalDate.of(2026, 6, 1), PaymentStatus.PENDING); // well beyond horizon

    when(leaseRepository.findAllByStatus(LeaseStatus.ACTIVE)).thenReturn(List.of(lease));
    when(paymentRepository.findTopByLeaseIdOrderByDueDateDesc(leaseId))
        .thenReturn(Optional.of(latest));
    when(paymentRepository.findAllByStatusAndDueDateLessThanEqualAndReminderSentAtIsNull(
            any(), any()))
        .thenReturn(List.of());

    job.runDailyJob();

    verify(paymentRepository, never()).save(any());
  }

  @Test
  void doesNotGenerateBeyondTheLeaseEndDate() {
    UUID leaseId = UUID.randomUUID();
    // Lease ends before the next monthly payment would fall due.
    Lease lease = activeLease(leaseId, UUID.randomUUID(), LocalDate.of(2026, 3, 15));
    Payment latest = payment(leaseId, LocalDate.of(2026, 3, 2), PaymentStatus.PAID);

    when(leaseRepository.findAllByStatus(LeaseStatus.ACTIVE)).thenReturn(List.of(lease));
    when(paymentRepository.findTopByLeaseIdOrderByDueDateDesc(leaseId))
        .thenReturn(Optional.of(latest));
    when(paymentRepository.findAllByStatusAndDueDateLessThanEqualAndReminderSentAtIsNull(
            any(), any()))
        .thenReturn(List.of());

    job.runDailyJob();

    verify(paymentRepository, never()).save(any());
  }

  @Test
  void skipsLeasesWithNoExistingPayment() {
    UUID leaseId = UUID.randomUUID();
    Lease lease = activeLease(leaseId, UUID.randomUUID(), LocalDate.of(2027, 1, 1));

    when(leaseRepository.findAllByStatus(LeaseStatus.ACTIVE)).thenReturn(List.of(lease));
    when(paymentRepository.findTopByLeaseIdOrderByDueDateDesc(leaseId))
        .thenReturn(Optional.empty());
    when(paymentRepository.findAllByStatusAndDueDateLessThanEqualAndReminderSentAtIsNull(
            any(), any()))
        .thenReturn(List.of());

    job.runDailyJob();

    verify(paymentRepository, never()).save(any());
  }

  @Test
  void sendsReminderAndMarksItSentForADuePayment() {
    UUID leaseId = UUID.randomUUID();
    UUID tenantId = UUID.randomUUID();
    Lease lease = activeLease(leaseId, tenantId, LocalDate.of(2027, 1, 1));
    Payment due = payment(leaseId, LocalDate.of(2026, 3, 3), PaymentStatus.PENDING);
    User tenant = new User();
    tenant.setId(tenantId);
    tenant.setEmail("tenant@example.com");
    tenant.setFullName("Sara Tenant");
    tenant.setRole(Role.TENANT);

    when(leaseRepository.findAllByStatus(LeaseStatus.ACTIVE)).thenReturn(List.of());
    when(paymentRepository.findAllByStatusAndDueDateLessThanEqualAndReminderSentAtIsNull(
            eq(PaymentStatus.PENDING), any()))
        .thenReturn(List.of(due));
    when(leaseRepository.findById(leaseId)).thenReturn(Optional.of(lease));
    when(userRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

    job.runDailyJob();

    verify(emailSender).send(eq("tenant@example.com"), any(), any());
    assertThat(due.getReminderSentAt()).isEqualTo(clock.instant());
    verify(paymentRepository, times(1)).save(due);
  }

  @Test
  void skipsGracefullyWhenTheLeaseOrTenantNoLongerExists() {
    UUID leaseId = UUID.randomUUID();
    Payment due = payment(leaseId, LocalDate.of(2026, 3, 3), PaymentStatus.PENDING);

    when(leaseRepository.findAllByStatus(LeaseStatus.ACTIVE)).thenReturn(List.of());
    when(paymentRepository.findAllByStatusAndDueDateLessThanEqualAndReminderSentAtIsNull(
            any(), any()))
        .thenReturn(List.of(due));
    when(leaseRepository.findById(leaseId)).thenReturn(Optional.empty());

    job.runDailyJob();

    verify(emailSender, never()).send(any(), any(), any());
    verify(paymentRepository, never()).save(any());
  }
}
