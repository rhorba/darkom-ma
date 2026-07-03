import { TestBed } from '@angular/core/testing';
import { Router, UrlTree } from '@angular/router';

import { UserSummary } from './auth.model';
import { AuthService } from './auth.service';
import { roleGuard } from './role.guard';

describe('roleGuard', () => {
  let currentUser: UserSummary | null;
  let router: Router;

  beforeEach(() => {
    currentUser = null;
    const authServiceSpy = jasmine.createSpyObj('AuthService', [], {
      currentUser: () => currentUser
    });

    TestBed.configureTestingModule({
      providers: [{ provide: AuthService, useValue: authServiceSpy }]
    });
    router = TestBed.inject(Router);
  });

  function runGuard(allowedRoles: Parameters<typeof roleGuard>[0]): boolean | UrlTree {
    const guard = roleGuard(allowedRoles);
    return TestBed.runInInjectionContext(() => guard(null as never, null as never)) as
      | boolean
      | UrlTree;
  }

  it('redirects to /login when there is no current user', () => {
    const result = runGuard(['ADMIN']);
    expect((result as UrlTree).toString()).toBe(router.createUrlTree(['/login']).toString());
  });

  it('allows navigation when the role is allowed', () => {
    currentUser = { id: 'u1', email: 'a@example.com', fullName: 'A', role: 'ADMIN' };
    expect(runGuard(['ADMIN'])).toBeTrue();
  });

  it("redirects to the user's own home when the role is not allowed", () => {
    currentUser = { id: 'u1', email: 't@example.com', fullName: 'T', role: 'TENANT' };
    const result = runGuard(['ADMIN']);
    expect((result as UrlTree).toString()).toBe(router.createUrlTree(['/my-lease']).toString());
  });
});
