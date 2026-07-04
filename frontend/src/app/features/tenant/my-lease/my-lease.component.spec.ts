import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';

import { API_BASE_URL } from '../../../core/config/api.config';
import { Lease } from '../../../core/leases/lease.model';
import { Payment } from '../../../core/payments/payment.model';
import { MyLeaseComponent } from './my-lease.component';

describe('MyLeaseComponent', () => {
  let fixture: ComponentFixture<MyLeaseComponent>;
  let component: MyLeaseComponent;
  let httpMock: HttpTestingController;
  let snackBarSpy: jasmine.SpyObj<MatSnackBar>;
  let routerSpy: jasmine.SpyObj<Router>;

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

  const pendingPayment: Payment = {
    id: 'p1',
    leaseId: 'l1',
    amount: 3500,
    dueDate: '2026-01-01',
    paidAt: null,
    status: 'PENDING',
    cmiTransactionId: null
  };

  function setUp(queryParams: Record<string, string> = {}): void {
    snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    TestBed.configureTestingModule({
      imports: [MyLeaseComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: MatSnackBar, useValue: snackBarSpy },
        { provide: Router, useValue: routerSpy },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: convertToParamMap(queryParams) } }
        }
      ]
    });

    fixture = TestBed.createComponent(MyLeaseComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  }

  afterEach(() => httpMock.verify());

  it('loads the active lease and its payment history', () => {
    setUp();
    fixture.detectChanges();

    httpMock.expectOne(`${API_BASE_URL}/api/v1/leases/mine`).flush(lease);
    httpMock.expectOne(`${API_BASE_URL}/api/v1/leases/l1/payments`).flush([pendingPayment]);

    expect(component.lease()).toEqual(lease);
    expect(component.payments()).toEqual([pendingPayment]);
    expect(component.nextPendingPayment()).toEqual(pendingPayment);
  });

  it('sets hasNoActiveLease when the tenant has no active lease', () => {
    setUp();
    fixture.detectChanges();

    httpMock
      .expectOne(`${API_BASE_URL}/api/v1/leases/mine`)
      .flush({ message: 'No active lease found' }, { status: 404, statusText: 'Not Found' });

    expect(component.hasNoActiveLease()).toBeTrue();
  });

  it('shows a success message and clears the query param when returning from a payment', () => {
    setUp({ payment: 'success' });
    fixture.detectChanges();

    httpMock.expectOne(`${API_BASE_URL}/api/v1/leases/mine`).flush(lease);
    httpMock.expectOne(`${API_BASE_URL}/api/v1/leases/l1/payments`).flush([]);

    expect(snackBarSpy.open).toHaveBeenCalledWith(
      'Paiement effectué avec succès.',
      'OK',
      jasmine.any(Object)
    );
    expect(routerSpy.navigate).toHaveBeenCalledWith([], { queryParams: {} });
  });

  it('shows a failure message when returning from a failed payment', () => {
    setUp({ payment: 'failed' });
    fixture.detectChanges();

    httpMock.expectOne(`${API_BASE_URL}/api/v1/leases/mine`).flush(lease);
    httpMock.expectOne(`${API_BASE_URL}/api/v1/leases/l1/payments`).flush([]);

    expect(snackBarSpy.open).toHaveBeenCalledWith('Le paiement a échoué.', 'OK', jasmine.any(Object));
  });

  it('redirects the browser to the CMI redirect URL when paying now', () => {
    setUp();
    fixture.detectChanges();
    httpMock.expectOne(`${API_BASE_URL}/api/v1/leases/mine`).flush(lease);
    httpMock.expectOne(`${API_BASE_URL}/api/v1/leases/l1/payments`).flush([pendingPayment]);

    const redirectSpy = spyOn(
      component as unknown as { redirectTo: (url: string) => void },
      'redirectTo'
    );

    component.payNow();
    httpMock
      .expectOne(`${API_BASE_URL}/api/v1/payments/l1/initiate`)
      .flush({ paymentId: 'p1', cmiTransactionId: 'txn-1', redirectUrl: 'http://x/mock-cmi/pay/txn-1' });

    expect(redirectSpy).toHaveBeenCalledWith('http://x/mock-cmi/pay/txn-1');
  });
});
