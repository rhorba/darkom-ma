import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { API_BASE_URL } from '../config/api.config';
import { AdminUser } from './admin-user.model';
import { AdminUserService } from './admin-user.service';

describe('AdminUserService', () => {
  let service: AdminUserService;
  let httpMock: HttpTestingController;

  const user: AdminUser = {
    id: 'u1',
    email: 'tenant@example.com',
    fullName: 'Test User',
    role: 'TENANT',
    active: true
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(AdminUserService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('lists all users', () => {
    service.list().subscribe((result) => expect(result).toEqual([user]));
    const req = httpMock.expectOne(`${API_BASE_URL}/api/v1/admin/users`);
    expect(req.request.method).toBe('GET');
    req.flush([user]);
  });

  it('deactivates a user', () => {
    const deactivated = { ...user, active: false };
    service.deactivate('u1').subscribe((result) => expect(result).toEqual(deactivated));
    const req = httpMock.expectOne(`${API_BASE_URL}/api/v1/admin/users/u1/deactivate`);
    expect(req.request.method).toBe('PATCH');
    req.flush(deactivated);
  });
});
