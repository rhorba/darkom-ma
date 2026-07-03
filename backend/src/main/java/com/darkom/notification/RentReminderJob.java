package com.darkom.notification;

import com.darkom.auth.entity.User;
import com.darkom.auth.repository.UserRepository;
import com.darkom.lease.entity.Lease;
import com.darkom.lease.entity.LeaseStatus;
import com.darkom.lease.repository.LeaseRepository;
import com.darkom.payment.entity.Payment;
import com.darkom.payment.entity.PaymentStatus;
import com.darkom.payment.repository.PaymentRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Daily batch job (SDR-3: Spring {@code @Scheduled}, no message broker - reminders are a daily
 * batch, not real-time). Two jobs in one run since the second is meaningless without the first:
 * generate the next month's PENDING payment once a lease's latest payment falls inside the reminder
 * window, then send (and mark) reminders for PENDING payments due soon.
 */
@Component
public class RentReminderJob {

  private static final Logger log = LoggerFactory.getLogger(RentReminderJob.class);

  private final LeaseRepository leaseRepository;
  private final PaymentRepository paymentRepository;
  private final UserRepository userRepository;
  private final EmailSender emailSender;
  private final RentReminderProperties properties;
  private final Clock clock;

  public RentReminderJob(
      LeaseRepository leaseRepository,
      PaymentRepository paymentRepository,
      UserRepository userRepository,
      EmailSender emailSender,
      RentReminderProperties properties,
      Clock clock) {
    this.leaseRepository = leaseRepository;
    this.paymentRepository = paymentRepository;
    this.userRepository = userRepository;
    this.emailSender = emailSender;
    this.properties = properties;
    this.clock = clock;
  }

  @Scheduled(cron = "${app.reminders.cron}")
  public void run() {
    runDailyJob();
  }

  @Transactional
  void runDailyJob() {
    generateUpcomingPayments();
    sendReminders();
  }

  private void generateUpcomingPayments() {
    LocalDate horizon = LocalDate.now(clock).plusDays(properties.getDaysBeforeDue());

    for (Lease lease : leaseRepository.findAllByStatus(LeaseStatus.ACTIVE)) {
      paymentRepository
          .findTopByLeaseIdOrderByDueDateDesc(lease.getId())
          .filter(latest -> !latest.getDueDate().isAfter(horizon))
          .map(latest -> latest.getDueDate().plusMonths(1))
          .filter(nextDueDate -> !nextDueDate.isAfter(lease.getEndDate()))
          .ifPresent(nextDueDate -> createNextPayment(lease, nextDueDate));
    }
  }

  private void createNextPayment(Lease lease, LocalDate dueDate) {
    Instant now = clock.instant();
    Payment payment = new Payment();
    payment.setId(UUID.randomUUID());
    payment.setLeaseId(lease.getId());
    payment.setAmount(lease.getMonthlyRent());
    payment.setDueDate(dueDate);
    payment.setStatus(PaymentStatus.PENDING);
    payment.setCreatedAt(now);
    payment.setUpdatedAt(now);
    paymentRepository.save(payment);
  }

  private void sendReminders() {
    LocalDate horizon = LocalDate.now(clock).plusDays(properties.getDaysBeforeDue());
    List<Payment> due =
        paymentRepository.findAllByStatusAndDueDateLessThanEqualAndReminderSentAtIsNull(
            PaymentStatus.PENDING, horizon);

    for (Payment payment : due) {
      leaseRepository
          .findById(payment.getLeaseId())
          .flatMap(lease -> userRepository.findById(lease.getTenantId()))
          .ifPresentOrElse(
              tenant -> sendReminderAndMark(payment, tenant),
              () ->
                  log.warn(
                      "Skipping reminder for payment {} - lease or tenant no longer exists",
                      payment.getId()));
    }
  }

  private void sendReminderAndMark(Payment payment, User tenant) {
    emailSender.send(
        tenant.getEmail(),
        "Rappel de paiement de loyer",
        "Bonjour "
            + tenant.getFullName()
            + ",\n\nUn paiement de "
            + payment.getAmount()
            + " MAD est du le "
            + payment.getDueDate()
            + ".\n\nMerci de proceder au paiement depuis votre espace Darkom.ma.");

    payment.setReminderSentAt(clock.instant());
    payment.setUpdatedAt(clock.instant());
    paymentRepository.save(payment);
  }
}
