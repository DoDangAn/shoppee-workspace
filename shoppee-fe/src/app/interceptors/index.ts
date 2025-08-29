import { HTTP_INTERCEPTORS } from '@angular/common/http';
import { authInterceptor } from './auth-interceptor';
import { apiInterceptor } from './api-interceptor';

export const httpInterceptorProviders = [
  { provide: HTTP_INTERCEPTORS, useValue: apiInterceptor, multi: true },
  { provide: HTTP_INTERCEPTORS, useValue: authInterceptor, multi: true }
];
