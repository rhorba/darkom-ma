import { Component, OnInit, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { MatToolbarModule } from '@angular/material/toolbar';

import { MaintenanceRequest, MaintenanceRequestStatus } from '../../../core/maintenance/maintenance.model';
import { MaintenanceService } from '../../../core/maintenance/maintenance.service';

@Component({
  selector: 'app-maintenance',
  imports: [MatButtonModule, MatSelectModule, MatTableModule, MatToolbarModule],
  templateUrl: './maintenance.component.html',
  styleUrl: './maintenance.component.scss'
})
export class MaintenanceComponent implements OnInit {
  private readonly maintenanceService = inject(MaintenanceService);
  private readonly snackBar = inject(MatSnackBar);

  readonly requests = signal<MaintenanceRequest[]>([]);
  readonly statuses: MaintenanceRequestStatus[] = ['OPEN', 'IN_PROGRESS', 'RESOLVED'];
  readonly displayedColumns = ['description', 'status', 'photo', 'createdAt'];

  ngOnInit(): void {
    this.reload();
  }

  private reload(): void {
    this.maintenanceService.list().subscribe((requests) => this.requests.set(requests));
  }

  updateStatus(request: MaintenanceRequest, status: MaintenanceRequestStatus): void {
    this.maintenanceService.updateStatus(request.id, status).subscribe(() => {
      this.snackBar.open('Statut mis à jour', 'OK', { duration: 3000 });
      this.reload();
    });
  }

  viewPhoto(request: MaintenanceRequest): void {
    this.maintenanceService.getPhotoBlob(request.id).subscribe((blob) => {
      window.open(URL.createObjectURL(blob), '_blank');
    });
  }
}
