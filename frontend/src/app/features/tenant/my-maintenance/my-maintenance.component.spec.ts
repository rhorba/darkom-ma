import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';

import { API_BASE_URL } from '../../../core/config/api.config';
import { Lease } from '../../../core/leases/lease.model';
import { MaintenanceRequest } from '../../../core/maintenance/maintenance.model';
import { MyMaintenanceComponent } from './my-maintenance.component';

describe('MyMaintenanceComponent', () => {
  let fixture: ComponentFixture<MyMaintenanceComponent>;
  let component: MyMaintenanceComponent;
  let httpMock: HttpTestingController;
  let snackBarSpy: jasmine.SpyObj<MatSnackBar>;

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

  const request: MaintenanceRequest = {
    id: 'm1',
    unitId: 'u1',
    reportedBy: 't1',
    description: 'Leaking faucet',
    hasPhoto: false,
    status: 'OPEN',
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z'
  };

  beforeEach(() => {
    snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    TestBed.configureTestingModule({
      imports: [MyMaintenanceComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: MatSnackBar, useValue: snackBarSpy }
      ]
    });
    fixture = TestBed.createComponent(MyMaintenanceComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  function initWithLeaseAndRequests(): void {
    fixture.detectChanges();
    httpMock.expectOne(`${API_BASE_URL}/api/v1/leases/mine`).flush(lease);
    httpMock.expectOne(`${API_BASE_URL}/api/v1/maintenance/mine`).flush([request]);
  }

  it('loads the tenant unit id and existing requests', () => {
    initWithLeaseAndRequests();
    expect(component.requests()).toEqual([request]);
  });

  it('does not submit an invalid form', () => {
    initWithLeaseAndRequests();
    component.submit();
    httpMock.expectNone(`${API_BASE_URL}/api/v1/maintenance`);
    expect(component.form.touched).toBeTrue();
  });

  it('submits a new request and reloads the list', () => {
    initWithLeaseAndRequests();
    component.form.controls.description.setValue('Leaking faucet');

    component.submit();

    const req = httpMock.expectOne(`${API_BASE_URL}/api/v1/maintenance`);
    expect(req.request.method).toBe('POST');
    req.flush(request);

    httpMock.expectOne(`${API_BASE_URL}/api/v1/maintenance/mine`).flush([request, request]);

    expect(snackBarSpy.open).toHaveBeenCalled();
    expect(component.requests().length).toBe(2);
  });

  it('shows an error message when submission fails', () => {
    initWithLeaseAndRequests();
    component.form.controls.description.setValue('Leaking faucet');

    component.submit();

    httpMock
      .expectOne(`${API_BASE_URL}/api/v1/maintenance`)
      .flush({ message: 'error' }, { status: 500, statusText: 'Server Error' });

    expect(component.errorMessage()).not.toBeNull();
  });
});
