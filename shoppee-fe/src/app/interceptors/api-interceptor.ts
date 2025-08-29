import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Api } from '../services/api';
import { AuthService } from '../services/auth';

export const apiInterceptor: HttpInterceptorFn = (req, next) => {
  // Only prefix relative /api requests (not already absolute)
  if (req.url.startsWith('/api/')) {
    // use the centralized Api helper to build/normalize target URLs
    const api = inject(Api);
    const endpoint = req.url.substring(4); // keep leading '/'
    // route admin endpoints to admin API helper
    if (endpoint.startsWith('/admin')) {
      // remove the '/admin' prefix and pass the remaining path to getAdminApiUrl
      const sub = endpoint.replace(/^\/admin/, '') || '/';
      const target = api.getAdminApiUrl(sub);
      try { console.log('[apiInterceptor] mapping admin path', endpoint, '->', target); } catch {}
      let apiReq = req.clone({ url: target });
      // fallback: if token exists, attach Authorization header here to guarantee backend sees it
      try {
        const auth = inject(AuthService);
        const token = auth.getToken();
        if (token) {
          apiReq = apiReq.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
          try { console.log('[apiInterceptor] attached Authorization header to admin request'); } catch {}
        } else {
          try { console.log('[apiInterceptor] no token available for admin request'); } catch {}
        }
      } catch (e) {
        // ignore injection errors in this context
      }
      try { console.log('[apiInterceptor] outgoing request url=', apiReq.url); } catch {}
      return next(apiReq);
    }
    const target = api.getUserApiUrl(endpoint);
    try { console.log('[apiInterceptor] mapping user path', endpoint, '->', target); } catch {}
    let apiReq = req.clone({ url: target });
    try {
      const auth = inject(AuthService);
      const token = auth.getToken();
      if (token) {
        apiReq = apiReq.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
        try { console.log('[apiInterceptor] attached Authorization header to user request'); } catch {}
      } else {
        try { console.log('[apiInterceptor] no token available for user request'); } catch {}
      }
    } catch (e) {}
    try { console.log('[apiInterceptor] outgoing request url=', apiReq.url); } catch {}
    return next(apiReq);
  }
  return next(req);
};
