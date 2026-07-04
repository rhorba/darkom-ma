import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../config/api.config';
import { AdminUser } from './admin-user.model';

@Injectable({ providedIn: 'root' })
export class AdminUserService {
  private readonly baseUrl = `${API_BASE_URL}/api/v1/admin/users`;

  constructor(private readonly http: HttpClient) {}

  list(): Observable<AdminUser[]> {
    return this.http.get<AdminUser[]>(this.baseUrl);
  }

  deactivate(id: string): Observable<AdminUser> {
    return this.http.patch<AdminUser>(`${this.baseUrl}/${id}/deactivate`, {});
  }
}
