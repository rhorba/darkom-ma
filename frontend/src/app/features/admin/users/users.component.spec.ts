import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';

import { API_BASE_URL } from '../../../core/config/api.config';
import { AdminUser } from '../../../core/admin/admin-user.model';
import { UsersComponent } from './users.component';

describe('UsersComponent', () => {
  let fixture: ComponentFixture<UsersComponent>;
  let component: UsersComponent;
  let httpMock: HttpTestingController;
  let snackBarSpy: jasmine.SpyObj<MatSnackBar>;

  const user: AdminUser = {
    id: 'u1',
    email: 'tenant@example.com',
    fullName: 'Test User',
    role: 'TENANT',
    active: true
  };

  beforeEach(() => {
    snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    TestBed.configureTestingModule({
      imports: [UsersComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: MatSnackBar, useValue: snackBarSpy }
      ]
    });
    fixture = TestBed.createComponent(UsersComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('loads the user list', () => {
    fixture.detectChanges();
    httpMock.expectOne(`${API_BASE_URL}/api/v1/admin/users`).flush([user]);

    expect(component.users()).toEqual([user]);
  });

  it('deactivates a user and reloads the list', () => {
    fixture.detectChanges();
    httpMock.expectOne(`${API_BASE_URL}/api/v1/admin/users`).flush([user]);

    component.deactivate(user);

    const patchReq = httpMock.expectOne(`${API_BASE_URL}/api/v1/admin/users/u1/deactivate`);
    expect(patchReq.request.method).toBe('PATCH');
    patchReq.flush({ ...user, active: false });

    httpMock.expectOne(`${API_BASE_URL}/api/v1/admin/users`).flush([{ ...user, active: false }]);

    expect(snackBarSpy.open).toHaveBeenCalled();
    expect(component.users()[0].active).toBeFalse();
  });
});
