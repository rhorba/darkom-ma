import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { API_BASE_URL } from '../config/api.config';
import { AuthResponse } from './auth.model';
import { AuthService } from './auth.service';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let authService: AuthService;

  const authResponse: AuthResponse = {
    accessToken: 'new-token',
    tokenType: 'Bearer',
    expiresIn: 900,
    user: { id: 'u1', email: 'a@example.com', fullName: 'A', role: 'TENANT' }
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting()
      ]
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    authService = TestBed.inject(AuthService);
  });

  afterEach(() => httpMock.verify());

  it('attaches the Authorization header when a token is present', () => {
    authService.login({ email: 'a@example.com', password: 'pw' }).subscribe();
    httpMock.expectOne(`${API_BASE_URL}/api/v1/auth/login`).flush(authResponse);

    http.get(`${API_BASE_URL}/api/v1/properties`).subscribe();
    const req = httpMock.expectOne(`${API_BASE_URL}/api/v1/properties`);

    expect(req.request.headers.get('Authorization')).toBe('Bearer new-token');
    expect(req.request.withCredentials).toBeTrue();
    req.flush({});
  });

  it('does not attach a stale token to the login request itself', () => {
    http.post(`${API_BASE_URL}/api/v1/auth/login`, {}).subscribe();
    const req = httpMock.expectOne(`${API_BASE_URL}/api/v1/auth/login`);

    expect(req.request.headers.has('Authorization')).toBeFalse();
    req.flush(authResponse);
  });

  it('retries once with a fresh token after a 401, then succeeds', () => {
    let result: unknown;
    http.get(`${API_BASE_URL}/api/v1/properties`).subscribe((res) => (result = res));

    const firstAttempt = httpMock.expectOne(`${API_BASE_URL}/api/v1/properties`);
    firstAttempt.flush(null, { status: 401, statusText: 'Unauthorized' });

    httpMock.expectOne(`${API_BASE_URL}/api/v1/auth/refresh`).flush(authResponse);

    const retriedAttempt = httpMock.expectOne(`${API_BASE_URL}/api/v1/properties`);
    expect(retriedAttempt.request.headers.get('Authorization')).toBe('Bearer new-token');
    retriedAttempt.flush({ ok: true });

    expect(result).toEqual({ ok: true });
  });

  it('propagates the original 401 when the refresh attempt also fails', () => {
    let capturedError: unknown;
    http.get(`${API_BASE_URL}/api/v1/properties`).subscribe({
      error: (err) => (capturedError = err)
    });

    httpMock
      .expectOne(`${API_BASE_URL}/api/v1/properties`)
      .flush(null, { status: 401, statusText: 'Unauthorized' });
    httpMock
      .expectOne(`${API_BASE_URL}/api/v1/auth/refresh`)
      .flush(null, { status: 401, statusText: 'Unauthorized' });

    expect((capturedError as { status: number }).status).toBe(401);
  });
});
