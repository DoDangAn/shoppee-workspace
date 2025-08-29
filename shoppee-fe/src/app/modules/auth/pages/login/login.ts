import { Component } from '@angular/core';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../../services/auth';
import { Router } from '@angular/router';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    MatInputModule,
    MatButtonModule,
    MatIconModule,
  MatCardModule,
  RouterLink,
  CommonModule,
    FormsModule
  ],
  templateUrl: './login.html',
  styleUrls: ['./login.scss']
})
export class Login {
  username: string = '';
  password: string = '';
  hide: boolean = true;
  loading: boolean = false;
  errorMessage: string | null = null;

  constructor(private auth: AuthService, private router: Router) {}

  onLogin(form: any) {
    if (form.invalid || this.loading) return;
    this.errorMessage = null;
    this.loading = true;
  this.auth.login({ username: this.username, password: this.password }).subscribe({
      next: res => {
        this.loading = false;
        const goAdmin = (res?.isAdmin) || this.auth.isAdminUser();
        this.router.navigateByUrl(goAdmin ? '/admin' : '/');
      },
      error: err => {
        this.loading = false;
        this.errorMessage = err?.error?.message || 'Đăng nhập thất bại';
      }
    });
  }
}
