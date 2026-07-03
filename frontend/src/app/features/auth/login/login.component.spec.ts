import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { API_BASE_URL } from '../../../core/config/api.config';
import { AuthResponse } from '../../../core/auth/auth.model';
import { LoginComponent } from './login.component';

describe('LoginComponent', () => {
  let fixture: ComponentFixture<LoginComponent>;
  let component: LoginComponent;
  let httpMock: HttpTestingController;
  let router: Router;

  const authResponse: AuthResponse = {
    accessToken: 'token',
    tokenType: 'Bearer',
    expiresIn: 900,
    user: { id: 'u1', email: 'a@example.com', fullName: 'A', role: 'TENANT' }
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LoginComponent, NoopAnimationsModule],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  afterEach(() => httpMock.verify());

  it('creates', () => {
    expect(component).toBeTruthy();
  });

  it('does not submit an invalid form', () => {
    component.form.setValue({ email: 'not-an-email', password: '' });
    component.submit();
    httpMock.expectNone(`${API_BASE_URL}/api/v1/auth/login`);
    expect(component.form.touched).toBeTrue();
  });

  it('navigates to the role home route on successful login', () => {
    const navigateSpy = spyOn(router, 'navigateByUrl');
    component.form.setValue({ email: 'a@example.com', password: 'supersecretpw' });

    component.submit();
    httpMock.expectOne(`${API_BASE_URL}/api/v1/auth/login`).flush(authResponse);

    expect(navigateSpy).toHaveBeenCalledWith('/my-lease');
    expect(component.isSubmitting()).toBeFalse();
  });

  it('shows an error message on invalid credentials', () => {
    component.form.setValue({ email: 'a@example.com', password: 'wrong-password' });

    component.submit();
    httpMock
      .expectOne(`${API_BASE_URL}/api/v1/auth/login`)
      .flush({ detail: 'Invalid' }, { status: 401, statusText: 'Unauthorized' });

    expect(component.errorMessage()).toBe('Email ou mot de passe incorrect.');
    expect(component.isSubmitting()).toBeFalse();
  });
});
