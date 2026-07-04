import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../config/api.config';
import { MaintenanceRequest, MaintenanceRequestStatus } from './maintenance.model';

@Injectable({ providedIn: 'root' })
export class MaintenanceService {
  private readonly baseUrl = `${API_BASE_URL}/api/v1/maintenance`;

  constructor(private readonly http: HttpClient) {}

  create(unitId: string, description: string, photo: File | null): Observable<MaintenanceRequest> {
    const formData = new FormData();
    formData.append('unitId', unitId);
    formData.append('description', description);
    if (photo) {
      formData.append('photo', photo);
    }
    return this.http.post<MaintenanceRequest>(this.baseUrl, formData);
  }

  listMine(): Observable<MaintenanceRequest[]> {
    return this.http.get<MaintenanceRequest[]>(`${this.baseUrl}/mine`);
  }

  list(): Observable<MaintenanceRequest[]> {
    return this.http.get<MaintenanceRequest[]>(this.baseUrl);
  }

  updateStatus(id: string, status: MaintenanceRequestStatus): Observable<MaintenanceRequest> {
    return this.http.patch<MaintenanceRequest>(`${this.baseUrl}/${id}`, { status });
  }

  getPhotoBlob(id: string): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/${id}/photo`, { responseType: 'blob' });
  }
}
