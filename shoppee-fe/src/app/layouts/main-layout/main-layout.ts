import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    RouterLink,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule
  ],
  template: `
    <mat-toolbar color="primary">
      <button mat-icon-button routerLink="/">
        <mat-icon>home</mat-icon>
      </button>
      <span>Shoppee</span>
      <span class="spacer"></span>
      <button mat-button routerLink="/product">
        <mat-icon>store</mat-icon>
        Sản phẩm
      </button>
      <button mat-button routerLink="/cart">
        <mat-icon>shopping_cart</mat-icon>
        Giỏ hàng
      </button>
      <button mat-button routerLink="/order">
        <mat-icon>receipt</mat-icon>
        Đơn hàng
      </button>
      <button mat-button routerLink="/auth/login">
        <mat-icon>person</mat-icon>
        Đăng nhập
      </button>
    </mat-toolbar>

    <div class="content">
      <router-outlet></router-outlet>
    </div>
  `,
  styles: [`
    .spacer {
      flex: 1 1 auto;
    }
    .content {
      padding: 20px;
      max-width: 1200px;
      margin: 0 auto;
    }
    mat-toolbar button {
      margin-right: 8px;
    }
  `]
})
export class MainLayout {}
