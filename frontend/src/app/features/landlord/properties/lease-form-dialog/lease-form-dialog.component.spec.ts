import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { API_BASE_URL } from '../../../../core/config/api.config';
import { Lease } from '../../../../core/leases/lease.model';
import { LeaseFormDialogComponent, LeaseFormDialogData } from './lease-form-dialog.component';

describe('LeaseFormDialogComponent', () => {
  let fixture: ComponentFixture<LeaseFormDialogComponent>;
  let component: LeaseFormDialogComponent;
  let httpMock: HttpTestingController;
  let dialogRefSpy: jasmine.SpyObj<MatDialogRef<LeaseFormDialogComponent>>;

  const dialogData: LeaseFormDialogData = { unitId: 'u1' };
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

  beforeEach(() => {
    dialogRefSpy = jasmine.createSpyObj('MatDialogRef', ['close']);

    TestBed.configureTestingModule({
      imports: [LeaseFormDialogComponent, NoopAnimationsModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: MatDialogRef, useValue: dialogRefSpy },
        { provide: MAT_DIALOG_DATA, useValue: dialogData }
      ]
    });

    fixture = TestBed.createComponent(LeaseFormDialogComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => httpMock.verify());

  function fillValidForm(): void {
    component.form.setValue({
      tenantEmail: 'tenant@example.com',
      startDate: '2026-01-01',
      endDate: '2026-12-31',
      monthlyRent: 3500
    });
  }

  it('does not submit an invalid form', () => {
    component.submit();
    httpMock.expectNone(`${API_BASE_URL}/api/v1/leases`);
    expect(component.form.touched).toBeTrue();
  });

  it('closes with the created lease on success', () => {
    fillValidForm();
    component.submit();

    httpMock.expectOne(`${API_BASE_URL}/api/v1/leases`).flush(lease);

    expect(dialogRefSpy.close).toHaveBeenCalledWith(lease);
    expect(component.isSubmitting()).toBeFalse();
  });

  it('shows an inline error on 409 and does not close', () => {
    fillValidForm();
    component.submit();

    httpMock
      .expectOne(`${API_BASE_URL}/api/v1/leases`)
      .flush(null, { status: 409, statusText: 'Conflict' });

    expect(component.errorMessage()).toBe('Ce lot a déjà un bail actif.');
    expect(dialogRefSpy.close).not.toHaveBeenCalled();
  });

  it('shows an inline error on 400 and does not close', () => {
    fillValidForm();
    component.submit();

    httpMock
      .expectOne(`${API_BASE_URL}/api/v1/leases`)
      .flush(null, { status: 400, statusText: 'Bad Request' });

    expect(component.errorMessage()).toBe("Vérifiez l'email du locataire et les dates du bail.");
    expect(dialogRefSpy.close).not.toHaveBeenCalled();
  });

  it('shows a generic error for anything else', () => {
    fillValidForm();
    component.submit();

    httpMock
      .expectOne(`${API_BASE_URL}/api/v1/leases`)
      .flush(null, { status: 500, statusText: 'Server Error' });

    expect(component.errorMessage()).toBe('Une erreur est survenue. Veuillez réessayer.');
  });

  it('closes with no result on cancel', () => {
    component.cancel();
    expect(dialogRefSpy.close).toHaveBeenCalledWith();
  });
});
