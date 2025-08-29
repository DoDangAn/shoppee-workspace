import { Component } from '@angular/core';
import { AuthService } from '../../../../services/auth';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatSidenavModule,
    MatToolbarModule,
    MatListModule,
    MatIconModule,
    MatButtonModule,
    MatMenuModule
  ],
  template: `
    <mat-sidenav-container class="admin-container">
      <mat-sidenav #sidenav mode="side" opened class="admin-sidenav">
        <mat-toolbar color="primary">
          <span>Admin Panel</span>
        </mat-toolbar>
        
        <mat-nav-list>
          <a mat-list-item routerLink="/admin/home" routerLinkActive="active">
            <mat-icon matListItemIcon>home</mat-icon>
            <span matListItemTitle>Home</span>
          </a>
          <a mat-list-item routerLink="/admin/dashboard" routerLinkActive="active">
            <mat-icon matListItemIcon>dashboard</mat-icon>
            <span matListItemTitle>Dashboard</span>
          </a>
          <a mat-list-item routerLink="/admin/products" routerLinkActive="active">
            <mat-icon matListItemIcon>inventory_2</mat-icon>
            <span matListItemTitle>Products</span>
          </a>
          <a mat-list-item routerLink="/admin/categories" routerLinkActive="active">
            <mat-icon matListItemIcon>category</mat-icon>
            <span matListItemTitle>Categories</span>
          </a>
          <a mat-list-item routerLink="/admin/orders" routerLinkActive="active">
            <mat-icon matListItemIcon>shopping_cart</mat-icon>
            <span matListItemTitle>Orders</span>
          </a>
          <a mat-list-item routerLink="/admin/users" routerLinkActive="active">
            <mat-icon matListItemIcon>people</mat-icon>
            <span matListItemTitle>Users</span>
          </a>
        </mat-nav-list>
      </mat-sidenav>

      <mat-sidenav-content>
        <mat-toolbar color="primary">
          <button mat-icon-button (click)="sidenav.toggle()">
            <mat-icon>menu</mat-icon>
          </button>
          <span class="toolbar-spacer"></span>
          <button mat-icon-button [matMenuTriggerFor]="userMenu">
            <mat-icon>account_circle</mat-icon>
          </button>
        </mat-toolbar>

        <div class="admin-content">
          <router-outlet></router-outlet>
        </div>
      </mat-sidenav-content>
    </mat-sidenav-container>

    <mat-menu #userMenu="matMenu">
      <button mat-menu-item routerLink="/profile">
        <mat-icon>person</mat-icon>
        <span>Profile</span>
      </button>
      <button mat-menu-item (click)="logout()">
        <mat-icon>exit_to_app</mat-icon>
        <span>Logout</span>
      </button>
    </mat-menu>
  `,
  styles: [`
    .admin-container {
      height: 100vh;
      width: 100vw;
    }

    .admin-sidenav {
      width: 250px;
      background-color: #fafafa;
    }

    .toolbar-spacer {
      flex: 1 1 auto;
    }

    .admin-content {
      padding: 20px;
    }

    mat-nav-list {
      .active {
        background-color: rgba(63, 81, 181, 0.1);
        color: #3f51b5;
        
        mat-icon {
          color: #3f51b5;
        }
      }
    }
  `]
})
export class AdminLayout {
  constructor(private auth: AuthService, private router: Router) {}
  logout() {
    this.auth.logout();
    this.router.navigateByUrl('/auth/login');
  }
}
