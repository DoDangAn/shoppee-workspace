import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';
import { Api } from './api';
import { AuthService } from './auth';
import { Router } from '@angular/router';

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  constructor(private http: HttpClient, private api: Api, private auth: AuthService, private router: Router) {}

  getUsers(): Observable<any> {
  const url = this.api.getAdminApiUrl('/users');
  console.log('[AdminService] getUsers request', { url, hasToken: !!this.auth.getToken() });
  const token = this.auth.getToken();
  const opts = token ? { headers: { Authorization: `Bearer ${token}` } } : undefined;
  return this.http.get(url, opts).pipe(
      catchError(err => {
        if (err?.status === 401) {
          // if we have a token, attempt refresh once; otherwise force logout
          if (this.auth.getToken()) {
            return this.auth.refreshProfile().pipe(switchMap(() => this.http.get(url, opts)),
              catchError(e => {
                // refresh failed -> logout and navigate to login
                try { this.auth.logout(); this.router.navigateByUrl('/auth/login'); } catch {}
                return throwError(() => e);
              }));
          }
          try { this.auth.logout(); this.router.navigateByUrl('/auth/login'); } catch {}
        }
        return throwError(() => err);
      })
    );
  }

  getOrders(): Observable<any> {
  const url = this.api.getAdminApiUrl('/orders');
  const token = this.auth.getToken();
  const opts = token ? { headers: { Authorization: `Bearer ${token}` } } : undefined;
  return this.http.get(url, opts).pipe(
      catchError(err => {
        if (err?.status === 401 && this.auth.getToken()) {
          return this.auth.refreshProfile().pipe(switchMap(() => this.http.get(url, opts)));
        }
        return throwError(() => err);
      })
    );
  }

  getProducts(): Observable<any> {
  const url = this.api.getAdminApiUrl('/products');
  const token = this.auth.getToken();
  const opts = token ? { headers: { Authorization: `Bearer ${token}` } } : undefined;
  return this.http.get(url, opts).pipe(
      catchError(err => {
        if (err?.status === 401 && this.auth.getToken()) {
          return this.auth.refreshProfile().pipe(switchMap(() => this.http.get(url, opts)));
        }
        return throwError(() => err);
      })
    );
  }

  getHome(): Observable<any> {
  const url = this.api.getAdminApiUrl('/home');
  const token = this.auth.getToken();
  const opts = token ? { headers: { Authorization: `Bearer ${token}` } } : undefined;
  return this.http.get(url, opts).pipe(
      catchError(err => {
        if (err?.status === 401 && this.auth.getToken()) {
          return this.auth.refreshProfile().pipe(switchMap(() => this.http.get(url, opts)));
        }
        return throwError(() => err);
      })
    );
  }

  // product variant CRUD (JSON-based endpoints)
  getProductVariants(): Observable<any> {
    const url = this.api.getAdminApiUrl('/product-variants');
    const token = this.auth.getToken();
    const opts = token ? { headers: { Authorization: `Bearer ${token}` } } : undefined;
    return this.http.get(url, opts);
  }

  createProductVariant(payload: any): Observable<any> {
    const url = this.api.getAdminApiUrl('/product-variants');
    const token = this.auth.getToken();
    const opts = token ? { headers: { Authorization: `Bearer ${token}` } } : undefined;
    return this.http.post(url, payload, opts);
  }

  updateProductVariant(id: string, payload: any): Observable<any> {
    const url = this.api.getAdminApiUrl(`/product-variants/${id}`);
    const token = this.auth.getToken();
    const opts = token ? { headers: { Authorization: `Bearer ${token}` } } : undefined;
    return this.http.put(url, payload, opts);
  }

  deleteProductVariant(id: string): Observable<any> {
    const url = this.api.getAdminApiUrl(`/product-variants/${id}`);
    const token = this.auth.getToken();
    const opts = token ? { headers: { Authorization: `Bearer ${token}` } } : undefined;
    return this.http.delete(url, opts);
  }

  // search products by name fragment (returns small list of { productID, productName })
  searchProducts(query: string): Observable<any> {
    const url = this.api.getAdminApiUrl(`/products/search?query=${encodeURIComponent(query)}`);
    const token = this.auth.getToken();
    const opts = token ? { headers: { Authorization: `Bearer ${token}` } } : undefined;
    return this.http.get(url, opts);
  }

  // search categories by name fragment (returns small list of { categoryID, categoryName })
  searchCategories(query: string): Observable<any> {
    const url = this.api.getAdminApiUrl(`/categories/search?query=${encodeURIComponent(query)}`);
    const token = this.auth.getToken();
    const opts = token ? { headers: { Authorization: `Bearer ${token}` } } : undefined;
    return this.http.get(url, opts);
  }

  // Add create/update methods later as backend supports them
}
