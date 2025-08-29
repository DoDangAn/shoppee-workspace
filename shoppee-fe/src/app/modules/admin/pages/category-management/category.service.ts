import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';
import { Api } from '../../../../services/api';
import { AuthService } from '../../../../services/auth';
import { Router } from '@angular/router';

export interface Category {
  id?: string;
  name: string;
  description?: string;
  status: 'Active' | 'Inactive';
  productsCount?: number;
}

@Injectable({
  providedIn: 'root'
})
export class CategoryService {
  constructor(private http: HttpClient, private api: Api, private auth: AuthService, private router: Router) {}

  getCategories(): Observable<Category[]> {
  const url = this.api.getAdminApiUrl('/categories');
  console.log('[CategoryService] getCategories request', { url, hasToken: !!this.auth.getToken() });
  const token = this.auth.getToken();
  const opts = token ? { headers: { Authorization: `Bearer ${token}` } } : undefined;
  return this.http.get<Category[]>(url, opts).pipe(
      catchError(err => {
        if (err?.status === 401) {
          if (this.auth.getToken()) {
            return this.auth.refreshProfile().pipe(switchMap(() => this.http.get<Category[]>(url, opts)), catchError(e => {
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

  getCategoryById(id: number): Observable<Category> {
  return this.http.get<Category>(this.api.getAdminApiUrl(`/category/${id}`));
  }

  createCategory(category: Category): Observable<Category> {
  const payload = { categoryName: category.name, description: category.description };
  return this.http.post<Category>(this.api.getAdminApiUrl('/category'), payload);
  }

  updateCategory(id: number, category: Category): Observable<Category> {
  const payload = { categoryName: category.name, description: category.description };
  return this.http.put<Category>(this.api.getAdminApiUrl(`/category/${id}`), payload);
  }

  deleteCategory(id: number): Observable<void> {
  return this.http.delete<void>(this.api.getAdminApiUrl(`/category/${id}`));
  }
}
