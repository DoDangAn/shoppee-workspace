import { HttpInterceptorFn, HttpRequest, HttpHandlerFn } from '@angular/common/http';

export const authInterceptor: HttpInterceptorFn = (req: HttpRequest<unknown>, next: HttpHandlerFn) => {
  // Get token from localStorage
  const token = localStorage.getItem('token');
  
  if (token) {
    // Clone the request and add the authorization header
    const authReq = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
    return next(authReq);
  }
  
  return next(req);
};
