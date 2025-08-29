import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { AdminService } from '../../../../services/admin';
import { AuthService } from '../../../../services/auth';
import { Router } from '@angular/router';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    MatCardModule,
    MatIconModule,
    MatToolbarModule
  ],
  templateUrl: './dashboard.html',
  styleUrls: ['./dashboard.scss']
})
export class DashboardPage implements OnInit {
  stats = { products: 0, orders: 0, users: 0 };

  constructor(
    private adminService: AdminService,
    private cdr: ChangeDetectorRef,
    private auth: AuthService,
    private router: Router
  ) {}

  ngOnInit() {
    if (this.auth.isAdminUser()) {
      this.fetchStats();
    } else if (this.auth.getToken()) {
      this.auth.refreshProfile().subscribe({
        next: () => { if (this.auth.isAdminUser()) this.fetchStats(); else this.router.navigateByUrl('/'); },
        error: () => this.router.navigateByUrl('/')
      });
    } else {
      this.router.navigateByUrl('/auth/login');
    }
  }

  private fetchStats() {
    this.adminService.getProducts().subscribe((data: any[]) => {
      this.stats.products = (data || []).length;
      try { this.cdr.detectChanges(); } catch (e) { }
    });
    this.adminService.getOrders().subscribe((data: any[]) => {
      this.stats.orders = (data || []).length;
      try { this.cdr.detectChanges(); } catch (e) { }
    });
    this.adminService.getUsers().subscribe((data: any[]) => {
      this.stats.users = (data || []).length;
      try { this.cdr.detectChanges(); } catch (e) { }
    });
  }
}
