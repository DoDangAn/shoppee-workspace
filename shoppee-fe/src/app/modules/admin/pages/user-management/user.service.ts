import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';
import { Api } from '../../../../services/api';
import { AuthService } from '../../../../services/auth';

export interface User {
  id?: number;
  username: string;
  email: string;
  fullName: string;
  role: 'ADMIN' | 'USER';
  status: 'Active' | 'Inactive';
  createdAt?: Date;
  lastLogin?: Date;
}

@Injectable({
  providedIn: 'root'
})
export class UserService {
  constructor(private http: HttpClient, private api: Api, private auth: AuthService) {}

  getUsers(): Observable<User[]> {
  const url = this.api.getAdminApiUrl('/users');
  const token = this.auth.getToken();
  const opts = token ? { headers: { Authorization: `Bearer ${token}` } } : undefined;
  return this.http.get<User[]>(url, opts).pipe(
      catchError(err => {
        if (err?.status === 401 && this.auth.getToken()) {
          return this.auth.refreshProfile().pipe(switchMap(() => this.http.get<User[]>(url, opts)));
        }
        return throwError(() => err);
      })
    );
  }

  getUserById(id: number): Observable<User> {
  return this.http.get<User>(this.api.getAdminApiUrl(`/user/${id}`));
  }

  updateUserStatus(id: number, status: string): Observable<User> {
  return this.http.patch<User>(this.api.getAdminApiUrl(`/user/${id}/status`), { status });
  }

  updateUserRole(id: number, role: string): Observable<User> {
  return this.http.patch<User>(this.api.getAdminApiUrl(`/user/${id}/role`), { role });
  }

  deleteUser(id: number): Observable<void> {
  return this.http.delete<void>(this.api.getAdminApiUrl(`/user/${id}`));
  }
}
