import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface User {
  id?: number;
  username: string;
  email: string;
  password?: string;
  role?: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  user: User;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'http://localhost:8080/api/auth'; // Spring Boot API URL

  constructor(private http: HttpClient) { }

  login(loginRequest: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, loginRequest);
  }

  register(user: User): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/register`, user);
  }

  logout(): Observable<any> {
    return this.http.post(`${this.apiUrl}/logout`, {});
  }

  getCurrentUser(): Observable<User> {
    return this.http.get<User>(`${this.apiUrl}/me`);
  }

  isAuthenticated(): boolean {
    return !!localStorage.getItem('token');
  }

  getToken(): string | null {
    return localStorage.getItem('token');
  }

  setToken(token: string): void {
    localStorage.setItem('token', token);
  }

  removeToken(): void {
    localStorage.removeItem('token');
  }
}
