import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { Api } from './api';

export interface LoginResponse {
  jwt: string;
  username: string;
  fullName?: string;
  isAdmin?: boolean;
  roles?: string[];
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private base = '/api/user'; // keep relative, api-interceptor will prefix
  private tokenKey = 'jwt_token';
  private userKey = 'auth_user';

  constructor(private http: HttpClient, private api: Api) {}

  login(credentials: { username: string; password: string }): Observable<LoginResponse> {
  return this.http.post<LoginResponse>(this.base + '/login', credentials).pipe(
      tap(res => {
        if (res?.jwt) {
          // store token in the primary key and legacy 'token' key (backend templates used 'token')
          localStorage.setItem(this.tokenKey, res.jwt);
          try { localStorage.setItem('token', res.jwt); } catch {}
          localStorage.setItem(this.userKey, JSON.stringify({
            username: res.username,
            fullName: res.fullName,
            isAdmin: res.isAdmin,
            roles: res.roles || []
          }));
        }
      })
    );
  }

  register(data: any): Observable<any> {
  return this.http.post(this.base + '/register', data);
  }

  getProfile(): Observable<any> {
  return this.http.get(this.base + '/profile');
  }

  // Refresh profile and persist latest roles/isAdmin
  refreshProfile(): Observable<any> {
    return this.getProfile().pipe(
      tap((res: any) => {
        if (res) {
          const u = this.getUser() || {};
          u.username = res.username || u.username;
          u.fullName = res.fullName || u.fullName;
            const rolesArr = res.roles || res.Roles || [];
          u.roles = rolesArr;
          u.isAdmin = rolesArr.includes('ADMIN');
          localStorage.setItem(this.userKey, JSON.stringify(u));
        }
      })
    );
  }

  promoteToAdmin(): Observable<any> {
  return this.http.post<any>(this.base + '/promote-admin', {}).pipe(
      tap((res: any) => {
        if (res && Array.isArray(res.roles)) {
          const u = this.getUser() || {};
          u.roles = res.roles;
          u.isAdmin = res.roles.includes('ADMIN');
          localStorage.setItem(this.userKey, JSON.stringify(u));
        }
      })
    );
  }

  // return primary token key, fallback to legacy 'token' key
  getToken(): string | null { return localStorage.getItem(this.tokenKey) || localStorage.getItem('token'); }
  isLoggedIn(): boolean { return !!this.getToken(); }
  getUser(): any { const raw = localStorage.getItem(this.userKey); return raw ? JSON.parse(raw) : null; }
  isAdminUser(): boolean { const u = this.getUser(); return !!(u && (u.isAdmin || (u.roles||[]).includes('ADMIN'))); }
  logout(): void {
    // remove both primary and legacy token keys
    try { localStorage.removeItem(this.tokenKey); } catch {}
    try { localStorage.removeItem('token'); } catch {}
    localStorage.removeItem(this.userKey);
  }
}
