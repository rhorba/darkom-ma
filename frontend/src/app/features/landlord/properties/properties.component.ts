import { Component, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { MatToolbarModule } from '@angular/material/toolbar';

import { Property } from '../../../core/properties/property.model';
import { PropertyService } from '../../../core/properties/property.service';
import { PropertyFormDialogComponent } from './property-form-dialog/property-form-dialog.component';

@Component({
  selector: 'app-properties',
  imports: [MatButtonModule, MatTableModule, MatToolbarModule],
  templateUrl: './properties.component.html',
  styleUrl: './properties.component.scss'
})
export class PropertiesComponent implements OnInit {
  private readonly propertyService = inject(PropertyService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly router = inject(Router);

  readonly properties = signal<Property[]>([]);
  readonly displayedColumns = ['name', 'city', 'status', 'actions'];

  ngOnInit(): void {
    this.reload();
  }

  private reload(): void {
    this.propertyService.list().subscribe((properties) => this.properties.set(properties));
  }

  openCreateDialog(): void {
    const dialogRef = this.dialog.open(PropertyFormDialogComponent, { data: null });
    dialogRef.afterClosed().subscribe((request) => {
      if (!request) {
        return;
      }
      this.propertyService.create(request).subscribe(() => {
        this.snackBar.open('Propriété créée', 'OK', { duration: 3000 });
        this.reload();
      });
    });
  }

  openDetail(property: Property): void {
    this.router.navigate(['/properties', property.id]);
  }

  archive(property: Property, event: Event): void {
    event.stopPropagation();
    this.propertyService.archive(property.id).subscribe(() => {
      this.snackBar.open('Propriété archivée', 'OK', { duration: 3000 });
      this.reload();
    });
  }
}
