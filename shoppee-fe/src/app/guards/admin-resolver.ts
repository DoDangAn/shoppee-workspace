import { ResolveFn } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth';
import { of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

// Resolver to ensure profile is refreshed before activating admin module routes.
export const adminResolver: ResolveFn<boolean> = () => {
  const auth = inject(AuthService);
  // If user already has roles loaded, resolve immediately
  if (auth.isAdminUser()) return of(true);
  if (auth.getToken()) {
    return auth.refreshProfile().pipe(
      map(() => true),
      catchError(() => of(false))
    );
  }
  return of(false);
};
