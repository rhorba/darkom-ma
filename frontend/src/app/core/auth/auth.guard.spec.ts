import { TestBed } from '@angular/core/testing';
import { Router, UrlTree } from '@angular/router';

import { authGuard } from './auth.guard';
import { AuthService } from './auth.service';

describe('authGuard', () => {
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let router: Router;

  beforeEach(() => {
    authServiceSpy = jasmine.createSpyObj('AuthService', [], { isAuthenticated: () => false });

    TestBed.configureTestingModule({
      providers: [{ provide: AuthService, useValue: authServiceSpy }]
    });
    router = TestBed.inject(Router);
  });

  function runGuard(): boolean | UrlTree {
    return TestBed.runInInjectionContext(() =>
      authGuard(null as never, null as never)
    ) as boolean | UrlTree;
  }

  it('allows navigation when authenticated', () => {
    Object.defineProperty(authServiceSpy, 'isAuthenticated', { value: () => true });
    expect(runGuard()).toBeTrue();
  });

  it('redirects to /login when not authenticated', () => {
    const result = runGuard();
    expect(result).not.toBeTrue();
    expect((result as UrlTree).toString()).toBe(router.createUrlTree(['/login']).toString());
  });
});
