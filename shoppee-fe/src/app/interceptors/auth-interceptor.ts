import { HttpInterceptorFn, HttpEvent } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap } from 'rxjs/operators';
import { throwError, of } from 'rxjs';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth';

const PUBLIC_PATHS = [
  '/api/products',      // list
  '/api/product',       // details/variants
  '/api/cart',          // temporary public cart
  '/api/user/login',
  '/api/user/register'
];

export const authInterceptor: HttpInterceptorFn = (req, next): import("rxjs").Observable<HttpEvent<any>> => {
  // Extract path without origin/query to do clean startsWith checks
  let path: string;
  try {
    const u = new URL(req.url, window.location.origin);
    path = u.pathname;
  } catch {
    // relative URL
    path = req.url.split('?')[0];
  }

  const isPublic = PUBLIC_PATHS.some(p => path === p || path.startsWith(p + '/'));
  // Prefer AuthService getter (handles legacy 'token' key), fallback to direct localStorage read
  let token: string | null = null;
  try {
    const auth = inject(AuthService);
    token = auth.getToken();
  } catch {
    token = localStorage.getItem('jwt_token') || localStorage.getItem('token');
  }

  const handle401 = (err: any, originalReq: any, originalNext: any): import("rxjs").Observable<HttpEvent<any>> | import("rxjs").Observable<never> => {
    // If 401, try to refresh profile (which will also refresh roles) and retry once
    if (err?.status === 401) {
      try { console.log('[authInterceptor] got 401 for path=', path); } catch {}
      try {
        const auth = inject(AuthService);
        const router = inject(Router);
        const token = auth.getToken();
        if (token) {
          // attempt to refresh profile and then retry original request with new token
          return auth.refreshProfile().pipe(
            switchMap(() => {
              const newToken = auth.getToken();
              const retried = originalReq.clone({ setHeaders: { Authorization: `Bearer ${newToken}` } });
              try { console.log('[authInterceptor] retried request after refresh for', path); } catch {}
              return originalNext(retried) as import("rxjs").Observable<HttpEvent<any>>;
            }),
            catchError((refreshErr: any) => {
              // refresh failed -> logout and redirect
              try { auth.logout(); router.navigateByUrl('/auth/login'); } catch (e) {}
              return throwError(() => err);
            })
          ) as import("rxjs").Observable<HttpEvent<any>>;
        } else {
          try { auth.logout(); router.navigateByUrl('/auth/login'); } catch (e) {}
        }
      } catch (e) {
        // injection failed -> fallback to logout
        try { const auth = inject(AuthService); const router = inject(Router); auth.logout(); router.navigateByUrl('/auth/login'); } catch (ex) {}
      }
    }
    return throwError(() => err);
  };

  if (isPublic) {
    return next(req).pipe(catchError((err) => handle401(err, req, next)));
  }

  if (token) {
    try { console.log('[authInterceptor] attaching Authorization header for path=', path, 'tokenPresent=', !!token); } catch {}
    // always set header when token exists to ensure backend receives it
    const cloned = req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
    try { console.log('[authInterceptor] outgoing request url=', cloned.url, 'headers=', cloned.headers.keys()); } catch {}
    return next(cloned).pipe(catchError((err) => handle401(err, cloned, next)));
  }

  try { console.log('[authInterceptor] no token found for path=', path); } catch {}
  return next(req).pipe(catchError((err) => handle401(err, req, next)));
};
