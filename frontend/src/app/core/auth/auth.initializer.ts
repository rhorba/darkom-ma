import { inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { AuthService } from './auth.service';

/**
 * Runs once at bootstrap so a page reload can silently recover a session from the refresh
 * cookie - the access token itself is memory-only and does not survive a reload on its own.
 */
export function initializeAuth(): Promise<boolean> {
  const authService = inject(AuthService);
  return firstValueFrom(authService.tryRestoreSession());
}
