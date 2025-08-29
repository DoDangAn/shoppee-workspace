import { Component, OnInit } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CommonModule, DatePipe } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../../../services/auth';
import { Router } from '@angular/router';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    MatCardModule,
    MatIconModule,
    MatProgressSpinnerModule,
    CommonModule,
    DatePipe
  ],
  templateUrl: './profile.html',
})
export class ProfilePage implements OnInit {
  profile: any;
  promoting = false;
  promoteMsg = '';

  constructor(private authService: AuthService, private router: Router) {}

  ngOnInit() {
    this.authService.getProfile().subscribe((data: any) => {
      this.profile = data;
    });
  }

  isAdmin() { const u = this.authService.getUser(); return !!(u && (u.isAdmin || (u.roles||[]).includes('ADMIN'))); }
  goAdmin() { this.router.navigateByUrl('/admin'); }
  promote() {
    if (this.promoting) return;
    this.promoting = true;
    this.promoteMsg='';
    this.authService.promoteToAdmin().subscribe({
      next: res => { this.promoteMsg = res?.message || 'Thành công'; this.promoting=false; },
      error: err => { this.promoteMsg = err?.error?.error || 'Lỗi'; this.promoting=false; }
    });
  }
}
