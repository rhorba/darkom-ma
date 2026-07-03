import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../config/api.config';
import { Lease, LeaseRequest } from './lease.model';

@Injectable({ providedIn: 'root' })
export class LeaseService {
  private readonly baseUrl = `${API_BASE_URL}/api/v1/leases`;

  constructor(private readonly http: HttpClient) {}

  create(request: LeaseRequest): Observable<Lease> {
    return this.http.post<Lease>(this.baseUrl, request);
  }

  get(id: string): Observable<Lease> {
    return this.http.get<Lease>(`${this.baseUrl}/${id}`);
  }

  downloadDocument(id: string): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/${id}/document`, { responseType: 'blob' });
  }
}
