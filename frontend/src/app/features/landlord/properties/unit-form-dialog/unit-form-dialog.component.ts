import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

import { Unit, UnitRequest } from '../../../../core/properties/property.model';

@Component({
  selector: 'app-unit-form-dialog',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule
  ],
  templateUrl: './unit-form-dialog.component.html',
  styleUrl: './unit-form-dialog.component.scss'
})
export class UnitFormDialogComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<UnitFormDialogComponent, UnitRequest>);
  readonly data = inject<Unit | null>(MAT_DIALOG_DATA);

  readonly form = this.formBuilder.group({
    label: [this.data?.label ?? '', [Validators.required]],
    monthlyRent: [this.data?.monthlyRent ?? null, [Validators.required, Validators.min(0.01)]]
  });

  get isEdit(): boolean {
    return this.data !== null;
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { label, monthlyRent } = this.form.getRawValue();
    this.dialogRef.close({ label: label!, monthlyRent: monthlyRent! });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
