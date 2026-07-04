import { Component, OnInit, ViewChild, ElementRef, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { MatToolbarModule } from '@angular/material/toolbar';

import { LeaseService } from '../../../core/leases/lease.service';
import { MaintenanceRequest } from '../../../core/maintenance/maintenance.model';
import { MaintenanceService } from '../../../core/maintenance/maintenance.service';

@Component({
  selector: 'app-my-maintenance',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatTableModule,
    MatToolbarModule
  ],
  templateUrl: './my-maintenance.component.html',
  styleUrl: './my-maintenance.component.scss'
})
export class MyMaintenanceComponent implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly leaseService = inject(LeaseService);
  private readonly maintenanceService = inject(MaintenanceService);
  private readonly snackBar = inject(MatSnackBar);

  @ViewChild('photoInput') private photoInput?: ElementRef<HTMLInputElement>;

  readonly form = this.formBuilder.group({
    description: ['', [Validators.required]]
  });

  readonly selectedPhoto = signal<File | null>(null);
  readonly requests = signal<MaintenanceRequest[]>([]);
  readonly isSubmitting = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly displayedColumns = ['description', 'status', 'createdAt'];

  private unitId: string | null = null;

  ngOnInit(): void {
    this.leaseService.getMine().subscribe((lease) => (this.unitId = lease.unitId));
    this.reloadRequests();
  }

  private reloadRequests(): void {
    this.maintenanceService.listMine().subscribe((requests) => this.requests.set(requests));
  }

  onPhotoSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedPhoto.set(input.files?.[0] ?? null);
  }

  submit(): void {
    if (this.form.invalid || !this.unitId) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set(null);

    const { description } = this.form.getRawValue();
    this.maintenanceService.create(this.unitId, description!, this.selectedPhoto()).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.form.reset();
        this.selectedPhoto.set(null);
        if (this.photoInput) {
          this.photoInput.nativeElement.value = '';
        }
        this.reloadRequests();
        this.snackBar.open('Demande envoyée', 'OK', { duration: 3000 });
      },
      error: () => {
        this.isSubmitting.set(false);
        this.errorMessage.set('Une erreur est survenue. Veuillez réessayer.');
      }
    });
  }
}
