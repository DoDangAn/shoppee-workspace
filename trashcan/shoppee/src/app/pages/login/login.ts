
import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth';

@Component({
  selector: 'app-login',
  standalone: false,
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class Login {
  username = '';
  password = '';
  loading = false;
  error: string | null = null;

  constructor(private authService: AuthService, private router: Router) {}

  onSubmit(): void {
    this.loading = true;
    this.error = null;
    this.authService.login({ username: this.username, password: this.password })
      .subscribe({
        next: (res) => {
          this.authService.setToken(res.token);
          this.loading = false;
          this.router.navigate(['/']);
        },
        error: (err) => {
          this.error = 'Đăng nhập thất bại. Vui lòng kiểm tra lại thông tin.';
          this.loading = false;
        }
      });
  }
}
