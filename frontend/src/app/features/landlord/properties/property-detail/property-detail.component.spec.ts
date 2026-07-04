import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { of } from 'rxjs';

import { API_BASE_URL } from '../../../../core/config/api.config';
import { Lease } from '../../../../core/leases/lease.model';
import { Property, Unit } from '../../../../core/properties/property.model';
import { PropertyDetailComponent } from './property-detail.component';

describe('PropertyDetailComponent', () => {
  let fixture: ComponentFixture<PropertyDetailComponent>;
  let component: PropertyDetailComponent;
  let httpMock: HttpTestingController;
  let dialogSpy: jasmine.SpyObj<MatDialog>;
  let snackBarSpy: jasmine.SpyObj<MatSnackBar>;

  const property: Property = {
    id: 'p1',
    name: 'Villa Zaytouna',
    address: '12 Rue des Oliviers',
    city: 'Rabat',
    archived: false
  };

  const unit: Unit = {
    id: 'u1',
    propertyId: 'p1',
    label: 'Apt 1',
    monthlyRent: 3500,
    status: 'VACANT',
    archived: false
  };

  beforeEach(() => {
    dialogSpy = jasmine.createSpyObj('MatDialog', ['open']);
    snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);

    TestBed.configureTestingModule({
      imports: [PropertyDetailComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: MatDialog, useValue: dialogSpy },
        { provide: MatSnackBar, useValue: snackBarSpy },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ id: 'p1' }) } }
        }
      ]
    });

    fixture = TestBed.createComponent(PropertyDetailComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('loads the property and its units on init', () => {
    fixture.detectChanges();
    httpMock.expectOne(`${API_BASE_URL}/api/v1/properties/p1`).flush(property);
    httpMock.expectOne(`${API_BASE_URL}/api/v1/properties/p1/units`).flush([unit]);

    expect(component.property()).toEqual(property);
    expect(component.units()).toEqual([unit]);
  });

  it('creates a unit and reloads the list when the dialog closes with a result', () => {
    fixture.detectChanges();
    httpMock.expectOne(`${API_BASE_URL}/api/v1/properties/p1`).flush(property);
    httpMock.expectOne(`${API_BASE_URL}/api/v1/properties/p1/units`).flush([]);

    dialogSpy.open.and.returnValue({
      afterClosed: () => of({ label: 'Apt 1', monthlyRent: 3500 })
    } as never);

    component.openCreateUnitDialog();

    httpMock.expectOne(`${API_BASE_URL}/api/v1/properties/p1/units`).flush(unit);
    httpMock.expectOne(`${API_BASE_URL}/api/v1/properties/p1/units`).flush([unit]);

    expect(snackBarSpy.open).toHaveBeenCalled();
    expect(component.units()).toEqual([unit]);
  });

  it('archives a unit and reloads the list', () => {
    fixture.detectChanges();
    httpMock.expectOne(`${API_BASE_URL}/api/v1/properties/p1`).flush(property);
    httpMock.expectOne(`${API_BASE_URL}/api/v1/properties/p1/units`).flush([unit]);

    component.archiveUnit(unit);

    httpMock.expectOne(`${API_BASE_URL}/api/v1/units/u1/archive`).flush({ ...unit, archived: true });
    httpMock.expectOne(`${API_BASE_URL}/api/v1/properties/p1/units`).flush([{ ...unit, archived: true }]);

    expect(component.units()[0].archived).toBeTrue();
  });

  it('creates a lease, reloads units, and downloads the generated PDF', () => {
    const lease: Lease = {
      id: 'l1',
      unitId: 'u1',
      tenantId: 't1',
      startDate: '2026-01-01',
      endDate: '2026-12-31',
      monthlyRent: 3500,
      status: 'ACTIVE',
      unitLabel: 'Apt 1',
      propertyName: 'Villa Zaytouna',
      propertyAddress: '12 Rue des Oliviers',
      propertyCity: 'Rabat'
    };

    fixture.detectChanges();
    httpMock.expectOne(`${API_BASE_URL}/api/v1/properties/p1`).flush(property);
    httpMock.expectOne(`${API_BASE_URL}/api/v1/properties/p1/units`).flush([unit]);

    dialogSpy.open.and.returnValue({ afterClosed: () => of(lease) } as never);

    component.openCreateLeaseDialog(unit);

    httpMock
      .expectOne(`${API_BASE_URL}/api/v1/properties/p1/units`)
      .flush([{ ...unit, status: 'OCCUPIED' }]);

    const documentReq = httpMock.expectOne(`${API_BASE_URL}/api/v1/leases/l1/document`);
    expect(documentReq.request.responseType).toBe('blob');
    documentReq.flush(new Blob(['%PDF-fake'], { type: 'application/pdf' }));

    expect(snackBarSpy.open).toHaveBeenCalled();
    expect(component.units()[0].status).toBe('OCCUPIED');
  });

  it('does nothing when the lease dialog is dismissed without a result', () => {
    fixture.detectChanges();
    httpMock.expectOne(`${API_BASE_URL}/api/v1/properties/p1`).flush(property);
    httpMock.expectOne(`${API_BASE_URL}/api/v1/properties/p1/units`).flush([unit]);

    dialogSpy.open.and.returnValue({ afterClosed: () => of(undefined) } as never);

    component.openCreateLeaseDialog(unit);

    httpMock.expectNone(() => true);
    expect(component.units()).toEqual([unit]);
  });
});
