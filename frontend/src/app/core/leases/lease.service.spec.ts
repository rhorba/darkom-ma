import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { API_BASE_URL } from '../config/api.config';
import { Payment } from '../payments/payment.model';
import { Lease } from './lease.model';
import { LeaseService } from './lease.service';

describe('LeaseService', () => {
  let service: LeaseService;
  let httpMock: HttpTestingController;

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
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(LeaseService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('creates a lease', () => {
    service
      .create({
        unitId: 'u1',
        tenantEmail: 'tenant@example.com',
        startDate: '2026-01-01',
        endDate: '2026-12-31',
        monthlyRent: 3500
      })
      .subscribe((result) => expect(result).toEqual(lease));

    const req = httpMock.expectOne(`${API_BASE_URL}/api/v1/leases`);
    expect(req.request.method).toBe('POST');
    req.flush(lease);
  });

  it('gets a lease by id', () => {
    service.get('l1').subscribe((result) => expect(result).toEqual(lease));
    httpMock.expectOne(`${API_BASE_URL}/api/v1/leases/l1`).flush(lease);
  });

  it('gets the current tenant active lease', () => {
    service.getMine().subscribe((result) => expect(result).toEqual(lease));
    const req = httpMock.expectOne(`${API_BASE_URL}/api/v1/leases/mine`);
    expect(req.request.method).toBe('GET');
    req.flush(lease);
  });

  it('lists payments for a lease', () => {
    const payments: Payment[] = [
      {
        id: 'p1',
        leaseId: 'l1',
        amount: 3500,
        dueDate: '2026-01-01',
        paidAt: null,
        status: 'PENDING',
        cmiTransactionId: null
      }
    ];
    service.listPayments('l1').subscribe((result) => expect(result).toEqual(payments));
    const req = httpMock.expectOne(`${API_BASE_URL}/api/v1/leases/l1/payments`);
    expect(req.request.method).toBe('GET');
    req.flush(payments);
  });

  it('downloads the lease document as a blob', () => {
    const pdfBlob = new Blob(['%PDF-fake'], { type: 'application/pdf' });
    service.downloadDocument('l1').subscribe((result) => expect(result).toEqual(pdfBlob));

    const req = httpMock.expectOne(`${API_BASE_URL}/api/v1/leases/l1/document`);
    expect(req.request.method).toBe('GET');
    expect(req.request.responseType).toBe('blob');
    req.flush(pdfBlob);
  });
});
