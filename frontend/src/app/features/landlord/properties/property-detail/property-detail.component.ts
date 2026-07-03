import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { MatToolbarModule } from '@angular/material/toolbar';

import { LeaseService } from '../../../../core/leases/lease.service';
import { triggerDownload } from '../../../../core/leases/trigger-download';
import { Property, Unit } from '../../../../core/properties/property.model';
import { PropertyService } from '../../../../core/properties/property.service';
import { LeaseFormDialogComponent } from '../lease-form-dialog/lease-form-dialog.component';
import { UnitFormDialogComponent } from '../unit-form-dialog/unit-form-dialog.component';

@Component({
  selector: 'app-property-detail',
  imports: [MatButtonModule, MatTableModule, MatToolbarModule],
  templateUrl: './property-detail.component.html',
  styleUrl: './property-detail.component.scss'
})
export class PropertyDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly propertyService = inject(PropertyService);
  private readonly leaseService = inject(LeaseService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  private readonly propertyId = this.route.snapshot.paramMap.get('id')!;

  readonly property = signal<Property | null>(null);
  readonly units = signal<Unit[]>([]);
  readonly displayedColumns = ['label', 'monthlyRent', 'status', 'actions'];

  ngOnInit(): void {
    this.propertyService.get(this.propertyId).subscribe((property) => this.property.set(property));
    this.reloadUnits();
  }

  private reloadUnits(): void {
    this.propertyService.listUnits(this.propertyId).subscribe((units) => this.units.set(units));
  }

  openCreateUnitDialog(): void {
    const dialogRef = this.dialog.open(UnitFormDialogComponent, { data: null });
    dialogRef.afterClosed().subscribe((request) => {
      if (!request) {
        return;
      }
      this.propertyService.createUnit(this.propertyId, request).subscribe(() => {
        this.snackBar.open('Lot créé', 'OK', { duration: 3000 });
        this.reloadUnits();
      });
    });
  }

  archiveUnit(unit: Unit): void {
    this.propertyService.archiveUnit(unit.id).subscribe(() => {
      this.snackBar.open('Lot archivé', 'OK', { duration: 3000 });
      this.reloadUnits();
    });
  }

  openCreateLeaseDialog(unit: Unit): void {
    const dialogRef = this.dialog.open(LeaseFormDialogComponent, { data: { unitId: unit.id } });
    dialogRef.afterClosed().subscribe((lease) => {
      if (!lease) {
        return;
      }
      this.reloadUnits();
      this.snackBar.open('Bail créé, téléchargement du PDF...', 'OK', { duration: 3000 });
      this.leaseService.downloadDocument(lease.id).subscribe((blob) => {
        triggerDownload(blob, `bail-${lease.id}.pdf`);
      });
    });
  }
}
