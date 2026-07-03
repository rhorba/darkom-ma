import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

import { Lease } from '../../../../core/leases/lease.model';
import { LeaseService } from '../../../../core/leases/lease.service';

export interface LeaseFormDialogData {
  unitId: string;
}

@Component({
  selector: 'app-lease-form-dialog',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule
  ],
  templateUrl: './lease-form-dialog.component.html',
  styleUrl: './lease-form-dialog.component.scss'
})
export class LeaseFormDialogComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<LeaseFormDialogComponent, Lease>);
  private readonly leaseService = inject(LeaseService);
  private readonly data = inject<LeaseFormDialogData>(MAT_DIALOG_DATA);

  readonly form = this.formBuilder.group({
    tenantEmail: ['', [Validators.required, Validators.email]],
    startDate: ['', [Validators.required]],
    endDate: ['', [Validators.required]],
    monthlyRent: [null as number | null, [Validators.required, Validators.min(0.01)]]
  });

  readonly isSubmitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set(null);

    const { tenantEmail, startDate, endDate, monthlyRent } = this.form.getRawValue();
    this.leaseService
      .create({
        unitId: this.data.unitId,
        tenantEmail: tenantEmail!,
        startDate: startDate!,
        endDate: endDate!,
        monthlyRent: monthlyRent!
      })
      .subscribe({
        next: (lease) => {
          this.isSubmitting.set(false);
          this.dialogRef.close(lease);
        },
        error: (err: HttpErrorResponse) => {
          this.isSubmitting.set(false);
          this.errorMessage.set(this.mapError(err));
        }
      });
  }

  cancel(): void {
    this.dialogRef.close();
  }

  private mapError(err: HttpErrorResponse): string {
    if (err.status === 409) {
      return 'Ce lot a déjà un bail actif.';
    }
    if (err.status === 400) {
      return "Vérifiez l'email du locataire et les dates du bail.";
    }
    return 'Une erreur est survenue. Veuillez réessayer.';
  }
}
