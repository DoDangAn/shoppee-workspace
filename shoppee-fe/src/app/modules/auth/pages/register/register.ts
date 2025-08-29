import { Component } from '@angular/core';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../../services/auth';
import { switchMap, catchError, finalize, throwError } from 'rxjs';
import { MatCardModule } from '@angular/material/card';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    MatInputModule,
    MatButtonModule,
    MatIconModule,
  MatCardModule,
  MatSelectModule,
  CommonModule,
    FormsModule
  ],
  templateUrl: './register.html',
  styleUrls: ['./register.scss']
})
export class RegisterPage {
  fullname = '';
  username = '';
  email = '';
  password = '';
  gender: boolean | null = null;
  birthday: string = '';
  adminCode: string = '';
  loading = false;
  error: string | null = null;

  constructor(private authService: AuthService, private router: Router) {}

  register(form: any) {
    if (form.invalid || this.loading) return;
    this.error = null;
    this.loading = true;
    const payload = {
      fullname: this.fullname,
      username: this.username,
      email: this.email,
      password: this.password,
      gender: this.gender,
  birthday: this.birthday,
  adminCode: this.adminCode || undefined
    };
    this.authService.register(payload).pipe(
      switchMap(() => this.authService.login({ username: this.username, password: this.password })),
      catchError(err => {
        this.error = err?.error?.error || err?.error?.message || 'Đăng ký hoặc đăng nhập thất bại';
        return throwError(() => err);
      }),
      finalize(() => this.loading = false)
    ).subscribe({
      next: (loginRes:any) => {
        const goAdmin = (loginRes?.isAdmin) || this.authService.isAdminUser();
        this.router.navigateByUrl(goAdmin ? '/admin' : '/');
      },
      error: () => {}
    });
  }
}
