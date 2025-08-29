import { Component, OnInit } from '@angular/core';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatBadgeModule } from '@angular/material/badge';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { CartService } from '../../services/cart';
import { AuthService } from '../../services/auth';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    RouterLinkActive,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatBadgeModule,
    MatMenuModule,
    MatDividerModule
  ],
  template: `
    <mat-toolbar color="primary" class="navbar">
      <div class="navbar-container">
        <div class="navbar-left">
          <button mat-icon-button routerLink="/">
            <mat-icon>storefront</mat-icon>
          </button>
          <span class="brand-name">Shoppee</span>
        </div>

        <div class="navbar-middle">
          <button mat-button routerLink="/" routerLinkActive="active" [routerLinkActiveOptions]="{exact: true}">
            <mat-icon>home</mat-icon>
            <span>Trang chủ</span>
          </button>
          <button mat-button routerLink="/product" routerLinkActive="active">
            <mat-icon>store</mat-icon>
            <span>Sản phẩm</span>
          </button>
          <button mat-button routerLink="/order" routerLinkActive="active">
            <mat-icon>receipt_long</mat-icon>
            <span>Đơn hàng</span>
          </button>
        </div>

        <div class="navbar-right">
          <button mat-button routerLink="/cart" routerLinkActive="active">
            <mat-icon [matBadge]="cartItemCount" matBadgeColor="warn" [matBadgeHidden]="cartItemCount === 0">
              shopping_cart
            </mat-icon>
            <span>Giỏ hàng</span>
          </button>
          <button mat-button routerLink="/auth/login" *ngIf="!isLoggedIn">
            <mat-icon>login</mat-icon>
            <span>Đăng nhập</span>
          </button>
          <button mat-button [matMenuTriggerFor]="userMenu" *ngIf="isLoggedIn">
            <mat-icon>person</mat-icon>
            <span>{{username}}</span>
          </button>
        </div>
      </div>
    </mat-toolbar>

    <mat-menu #userMenu="matMenu">
      <button mat-menu-item routerLink="/profile">
        <mat-icon>account_circle</mat-icon>
        <span>Tài khoản của tôi</span>
      </button>
      <button mat-menu-item routerLink="/order">
        <mat-icon>receipt</mat-icon>
        <span>Đơn hàng của tôi</span>
      </button>
      <mat-divider></mat-divider>
      <button mat-menu-item (click)="logout()">
        <mat-icon>logout</mat-icon>
        <span>Đăng xuất</span>
      </button>
    </mat-menu>
  `,
  styles: [`
    .navbar {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      z-index: 1000;
      padding: 0;
    }

    .navbar-container {
      width: 100%;
      max-width: 1200px;
      margin: 0 auto;
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 0 16px;
    }

    .navbar-left {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .brand-name {
      font-size: 1.5rem;
      font-weight: 600;
      margin-left: 8px;
      letter-spacing: 1px;
    }

    .navbar-middle {
      display: flex;
      gap: 8px;

      button {
        mat-icon {
          margin-right: 4px;
        }
      }
    }

    .navbar-right {
      display: flex;
      gap: 8px;
    }

    .active {
      background: rgba(255, 255, 255, 0.1);
    }

    @media (max-width: 768px) {
      .navbar-middle {
        display: none;
      }

      button span {
        display: none;
      }
    }
  `]
})
export class Navbar implements OnInit {
  cartItemCount = 0;
  isLoggedIn = false;
  username = '';
  constructor(private cartService: CartService, private auth: AuthService) {}

  ngOnInit(): void {
    // Cart count (backend returns array; service normalizes)
    this.cartService.items$.subscribe(list => this.cartItemCount = list.length);
    this.cartService.getCart().subscribe(); // ensure refresh
    this.refreshUser();
    // Listen to storage changes (other tabs)
    window.addEventListener('storage', () => this.refreshUser());
  }

  private refreshUser() {
    this.isLoggedIn = this.auth.isLoggedIn();
    const u = this.auth.getUser();
    this.username = u?.fullName || u?.username || '';
  }

  logout() {
    this.auth.logout();
    this.refreshUser();
  }
}
