import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from './auth';

/**
 * AdminGuard ensures that only authenticated users with ROLE_ADMIN
 * can access the Admin Panel. Regular users and guests are redirected to /home.
 */
export const adminGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isLoggedIn() && authService.isAdmin()) {
    return true;
  }

  // Stealth: Redirect unauthorized users back to the home store
  return router.createUrlTree(['/home']);
};
