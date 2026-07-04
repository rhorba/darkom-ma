import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { MatToolbarModule } from '@angular/material/toolbar';

import { triggerDownload } from '../../../core/leases/trigger-download';
import { Lease } from '../../../core/leases/lease.model';
import { LeaseService } from '../../../core/leases/lease.service';
import { Payment } from '../../../core/payments/payment.model';
import { PaymentService } from '../../../core/payments/payment.service';

@Component({
  selector: 'app-my-lease',
  imports: [MatButtonModule, MatTableModule, MatToolbarModule],
  templateUrl: './my-lease.component.html',
  styleUrl: './my-lease.component.scss'
})
export class MyLeaseComponent implements OnInit {
  private readonly leaseService = inject(LeaseService);
  private readonly paymentService = inject(PaymentService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);

  readonly lease = signal<Lease | null>(null);
  readonly payments = signal<Payment[]>([]);
  readonly hasNoActiveLease = signal(false);
  readonly displayedColumns = ['dueDate', 'amount', 'status', 'paidAt'];

  readonly nextPendingPayment = computed(() =>
    this.payments()
      .filter((p) => p.status === 'PENDING')
      .sort((a, b) => a.dueDate.localeCompare(b.dueDate))
      .at(0)
  );

  ngOnInit(): void {
    this.showPaymentOutcomeIfPresent();
    this.leaseService.getMine().subscribe({
      next: (lease) => {
        this.lease.set(lease);
        this.reloadPayments(lease.id);
      },
      error: () => this.hasNoActiveLease.set(true)
    });
  }

  private reloadPayments(leaseId: string): void {
    this.leaseService.listPayments(leaseId).subscribe((payments) => this.payments.set(payments));
  }

  private showPaymentOutcomeIfPresent(): void {
    const outcome = this.route.snapshot.queryParamMap.get('payment');
    if (!outcome) {
      return;
    }
    const message =
      outcome === 'success' ? 'Paiement effectué avec succès.' : 'Le paiement a échoué.';
    this.snackBar.open(message, 'OK', { duration: 5000 });
    this.router.navigate([], { queryParams: {} });
  }

  payNow(): void {
    const lease = this.lease();
    if (!lease) {
      return;
    }
    this.paymentService.initiate(lease.id).subscribe((response) => {
      this.redirectTo(response.redirectUrl);
    });
  }

  /** Full browser navigation to the mock CMI page - split out so tests can spy on it instead of
   * touching window.location, which isn't configurable in every browser. */
  protected redirectTo(url: string): void {
    window.location.href = url;
  }

  downloadDocument(): void {
    const lease = this.lease();
    if (!lease) {
      return;
    }
    this.leaseService.downloadDocument(lease.id).subscribe((blob) => {
      triggerDownload(blob, `bail-${lease.id}.pdf`);
    });
  }
}
