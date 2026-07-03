import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

import { Property, PropertyRequest } from '../../../../core/properties/property.model';

@Component({
  selector: 'app-property-form-dialog',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule
  ],
  templateUrl: './property-form-dialog.component.html',
  styleUrl: './property-form-dialog.component.scss'
})
export class PropertyFormDialogComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<PropertyFormDialogComponent, PropertyRequest>);
  readonly data = inject<Property | null>(MAT_DIALOG_DATA);

  readonly form = this.formBuilder.group({
    name: [this.data?.name ?? '', [Validators.required]],
    address: [this.data?.address ?? '', [Validators.required]],
    city: [this.data?.city ?? '', [Validators.required]]
  });

  get isEdit(): boolean {
    return this.data !== null;
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { name, address, city } = this.form.getRawValue();
    this.dialogRef.close({ name: name!, address: address!, city: city! });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
