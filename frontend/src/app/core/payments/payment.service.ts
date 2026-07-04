import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../config/api.config';
import { PaymentInitiationResponse } from './payment.model';

@Injectable({ providedIn: 'root' })
export class PaymentService {
  private readonly baseUrl = `${API_BASE_URL}/api/v1/payments`;

  constructor(private readonly http: HttpClient) {}

  initiate(leaseId: string): Observable<PaymentInitiationResponse> {
    return this.http.post<PaymentInitiationResponse>(`${this.baseUrl}/${leaseId}/initiate`, {});
  }
}
