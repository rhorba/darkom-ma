import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { ROLE_HOME_ROUTE, Role } from './auth.model';
import { AuthService } from './auth.service';

/** Redirects to /login if unauthenticated, or to the user's own home if their role isn't allowed. */
export function roleGuard(allowedRoles: Role[]): CanActivateFn {
  return () => {
    const authService = inject(AuthService);
    const router = inject(Router);
    const user = authService.currentUser();

    if (!user) {
      return router.createUrlTree(['/login']);
    }
    if (!allowedRoles.includes(user.role)) {
      return router.createUrlTree([ROLE_HOME_ROUTE[user.role]]);
    }
    return true;
  };
}
