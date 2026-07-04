import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { API_BASE_URL } from '../config/api.config';
import { PaymentInitiationResponse } from './payment.model';
import { PaymentService } from './payment.service';

describe('PaymentService', () => {
  let service: PaymentService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(PaymentService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('initiates a payment for a lease', () => {
    const response: PaymentInitiationResponse = {
      paymentId: 'p1',
      cmiTransactionId: 'txn-1',
      redirectUrl: 'http://localhost:8080/mock-cmi/pay/txn-1'
    };

    service.initiate('l1').subscribe((result) => expect(result).toEqual(response));

    const req = httpMock.expectOne(`${API_BASE_URL}/api/v1/payments/l1/initiate`);
    expect(req.request.method).toBe('POST');
    req.flush(response);
  });
});
