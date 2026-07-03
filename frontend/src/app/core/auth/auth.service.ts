import { HttpClient } from '@angular/common/http';
import { Injectable, computed, signal } from '@angular/core';
import { Observable, catchError, map, of, tap } from 'rxjs';

import { API_BASE_URL } from '../config/api.config';
import { AuthResponse, LoginRequest, UserSummary } from './auth.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly baseUrl = `${API_BASE_URL}/api/v1/auth`;

  /** In-memory only, per docs/security-darkom.md - never persisted (XSS blast-radius limit). */
  private accessToken: string | null = null;

  private readonly currentUserSignal = signal<UserSummary | null>(null);
  readonly currentUser = this.currentUserSignal.asReadonly();
  readonly isAuthenticated = computed(() => this.currentUserSignal() !== null);

  constructor(private readonly http: HttpClient) {}

  getAccessToken(): string | null {
    return this.accessToken;
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.baseUrl}/login`, request, { withCredentials: true })
      .pipe(tap((response) => this.applySession(response)));
  }

  refresh(): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.baseUrl}/refresh`, {}, { withCredentials: true })
      .pipe(tap((response) => this.applySession(response)));
  }

  /** Attempts to restore a session from the refresh cookie; never throws. */
  tryRestoreSession(): Observable<boolean> {
    return this.refresh().pipe(
      map(() => true),
      catchError(() => of(false))
    );
  }

  logout(): void {
    this.accessToken = null;
    this.currentUserSignal.set(null);
  }

  private applySession(response: AuthResponse): void {
    this.accessToken = response.accessToken;
    this.currentUserSignal.set(response.user);
  }
}
