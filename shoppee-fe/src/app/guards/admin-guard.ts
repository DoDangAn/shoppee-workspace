import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth';
import { of } from 'rxjs';
import { tap, map, catchError, switchMap } from 'rxjs/operators';

// Guard: Only allow if user is admin, otherwise redirect to home
export const adminGuard: CanActivateFn = (route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.isAdminUser()) return true;
  // If token exists but roles not loaded, try refresh once
  if (auth.getToken()) {
    return auth.refreshProfile().pipe(
      map(() => {
        if (auth.isAdminUser()) return true;
        router.navigateByUrl('/');
        return false;
      }),
      catchError(() => {
        router.navigateByUrl('/');
        return of(false);
      })
    );
  }
  router.navigateByUrl('/auth/login');
  return false;
};
