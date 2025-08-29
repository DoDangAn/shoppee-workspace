import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { AuthService } from '../../../../services/auth';
import { Router, NavigationEnd } from '@angular/router';
import { Subscription } from 'rxjs';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AdminService } from '../../../../services/admin';

@Component({
  selector: 'app-admin-home',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatProgressSpinnerModule],
  template: `
    <div class="page-container" *ngIf="!loading; else loadingTpl">
      <h1 class="page-title">Admin Home</h1>
      <div class="stats-grid" *ngIf="data">
        <mat-card class="stat-card">
          <mat-card-title>Sản phẩm</mat-card-title>
          <mat-card-content>{{data?.data?.totalProducts}}</mat-card-content>
        </mat-card>
        <mat-card class="stat-card">
          <mat-card-title>Danh mục</mat-card-title>
          <mat-card-content>{{data?.data?.totalCategories}}</mat-card-content>
        </mat-card>
        <mat-card class="stat-card">
          <mat-card-title>Người dùng</mat-card-title>
          <mat-card-content>{{data?.data?.totalUsers}}</mat-card-content>
        </mat-card>
      </div>
      <div *ngIf="error" class="error">{{error}}</div>
    </div>
    <ng-template #loadingTpl>
      <div class="loading-wrapper"><mat-spinner></mat-spinner></div>
    </ng-template>
  `,
  styles: [`
    .page-container { padding:20px; }
    .stats-grid { display:grid; gap:16px; grid-template-columns: repeat(auto-fill,minmax(180px,1fr)); }
    .stat-card { text-align:center; }
    .loading-wrapper { display:flex; justify-content:center; padding:40px; }
    .error { color:#d32f2f; margin-top:16px; }
  `]
})
export class AdminHomePage implements OnInit, OnDestroy {
  loading = false;
  data: any;
  error: string | null = null;
  private navSub?: Subscription;
  constructor(private adminService: AdminService, private router: Router, private cdr: ChangeDetectorRef, private auth: AuthService) {}

  ngOnInit() {
    if (this.auth.isAdminUser()) {
      this.fetchData();
    } else if (this.auth.getToken()) {
      this.auth.refreshProfile().subscribe({ next: () => { if (this.auth.isAdminUser()) this.fetchData(); else this.router.navigateByUrl('/'); }, error: () => this.router.navigateByUrl('/') });
    } else {
      this.router.navigateByUrl('/auth/login');
    }

    this.navSub = this.router.events.subscribe(event => {
      if (event instanceof NavigationEnd && this.router.url.includes('/admin/home')) {
        this.fetchData();
      }
    });
  }

  ngOnDestroy() {
    this.navSub?.unsubscribe();
  }

  private fetchData() {
    this.loading = true;
    this.adminService.getHome().subscribe({
      next: res => { this.data = res; this.loading = false; try { this.cdr.detectChanges(); } catch (e) { } },
      error: err => { this.error = err?.error?.error || 'Lỗi tải dữ liệu'; this.loading = false; }
    });
  }
}

