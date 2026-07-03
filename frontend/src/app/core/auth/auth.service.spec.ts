import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { API_BASE_URL } from '../config/api.config';
import { AuthResponse } from './auth.model';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  const authResponse: AuthResponse = {
    accessToken: 'access-token-123',
    tokenType: 'Bearer',
    expiresIn: 900,
    user: { id: 'u1', email: 'a@example.com', fullName: 'A', role: 'TENANT' }
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('starts unauthenticated with no access token', () => {
    expect(service.isAuthenticated()).toBeFalse();
    expect(service.getAccessToken()).toBeNull();
  });

  it('login stores the access token and current user on success', () => {
    service.login({ email: 'a@example.com', password: 'pw' }).subscribe();

    const req = httpMock.expectOne(`${API_BASE_URL}/api/v1/auth/login`);
    expect(req.request.withCredentials).toBeTrue();
    req.flush(authResponse);

    expect(service.getAccessToken()).toBe('access-token-123');
    expect(service.isAuthenticated()).toBeTrue();
    expect(service.currentUser()?.email).toBe('a@example.com');
  });

  it('login leaves state unauthenticated on failure', () => {
    service.login({ email: 'a@example.com', password: 'wrong' }).subscribe({ error: () => undefined });

    httpMock
      .expectOne(`${API_BASE_URL}/api/v1/auth/login`)
      .flush({ detail: 'Invalid' }, { status: 401, statusText: 'Unauthorized' });

    expect(service.isAuthenticated()).toBeFalse();
  });

  it('refresh updates the session on success', () => {
    service.refresh().subscribe();

    const req = httpMock.expectOne(`${API_BASE_URL}/api/v1/auth/refresh`);
    expect(req.request.withCredentials).toBeTrue();
    req.flush(authResponse);

    expect(service.isAuthenticated()).toBeTrue();
  });

  it('tryRestoreSession resolves true on a successful refresh', (done) => {
    service.tryRestoreSession().subscribe((restored) => {
      expect(restored).toBeTrue();
      done();
    });

    httpMock.expectOne(`${API_BASE_URL}/api/v1/auth/refresh`).flush(authResponse);
  });

  it('tryRestoreSession resolves false and swallows the error when there is no valid cookie', (done) => {
    service.tryRestoreSession().subscribe((restored) => {
      expect(restored).toBeFalse();
      done();
    });

    httpMock
      .expectOne(`${API_BASE_URL}/api/v1/auth/refresh`)
      .flush(null, { status: 401, statusText: 'Unauthorized' });
  });

  it('logout clears the in-memory session', () => {
    service.login({ email: 'a@example.com', password: 'pw' }).subscribe();
    httpMock.expectOne(`${API_BASE_URL}/api/v1/auth/login`).flush(authResponse);
    expect(service.isAuthenticated()).toBeTrue();

    service.logout();

    expect(service.isAuthenticated()).toBeFalse();
    expect(service.getAccessToken()).toBeNull();
    expect(service.currentUser()).toBeNull();
  });

  it('propagates the underlying HttpErrorResponse on login failure', () => {
    let captured: unknown;
    service.login({ email: 'a@example.com', password: 'wrong' }).subscribe({
      error: (err) => (captured = err)
    });

    httpMock
      .expectOne(`${API_BASE_URL}/api/v1/auth/login`)
      .flush({ detail: 'Invalid' }, { status: 401, statusText: 'Unauthorized' });

    expect(captured instanceof HttpErrorResponse).toBeTrue();
  });
});
