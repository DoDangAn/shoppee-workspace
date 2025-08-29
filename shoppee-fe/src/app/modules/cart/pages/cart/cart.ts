import { Component, OnInit } from '@angular/core';
import { MatTableModule } from '@angular/material/table';
import { MatListModule } from '@angular/material/list';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { CartService } from '../../../../services/cart';
import { OrderService } from '../../../../services/order';
import { Api } from '../../../../services/api';
import { environment } from 'environments/environment';
import { Router } from '@angular/router';
import { PageLayout } from '../../../../components/page-layout/page-layout';

@Component({
  selector: 'app-cart',
  standalone: true,
  imports: [
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatListModule,
    RouterLink,
    CommonModule,
    PageLayout,
  ],
  template: `
    <app-page-layout title="Giỏ hàng của bạn" icon="shopping_cart">
      <div class="table-container" *ngIf="cartItems.length > 0; else empty">
        <table mat-table [dataSource]="cartItems">
          <ng-container matColumnDef="name">
            <th mat-header-cell *matHeaderCellDef>Sản phẩm</th>
            <td mat-cell *matCellDef="let item; let i = index">
              <div class="prod-cell">
                <img *ngIf="item.image" [src]="resolveImage(item.image)" alt="{{item.name}}"> {{item.name}}
              </div>
            </td>
          </ng-container>
          <ng-container matColumnDef="quantity">
            <th mat-header-cell *matHeaderCellDef>Số lượng</th>
            <td mat-cell *matCellDef="let item; let i = index">
              <button mat-icon-button (click)="updateQuantity(i, -1)"><mat-icon>remove</mat-icon></button>
              {{item.quantity}}
              <button mat-icon-button (click)="updateQuantity(i, 1)"><mat-icon>add</mat-icon></button>
            </td>
          </ng-container>
          <ng-container matColumnDef="price">
            <th mat-header-cell *matHeaderCellDef>Giá</th>
            <td mat-cell *matCellDef="let item">{{item.price | currency:'VND'}}<br><small>x {{item.quantity}} = {{item.price * item.quantity | currency:'VND'}}</small></td>
          </ng-container>
          <ng-container matColumnDef="actions">
            <th mat-header-cell *matHeaderCellDef>Thao tác</th>
            <td mat-cell *matCellDef="let item; let i = index">
              <button mat-icon-button color="warn" (click)="removeItem(i)"><mat-icon>delete</mat-icon></button>
            </td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
          <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
        </table>
        <div class="button-group">
          <span class="spacer"></span>
            <h3>Tổng: {{getTotal() | currency:'VND'}}</h3>
          <button mat-raised-button color="primary" (click)="checkout()" [disabled]="cartItems.length===0 || !isLoggedIn"><mat-icon>shopping_cart_checkout</mat-icon> Đặt hàng</button>
        </div>
      </div>
      <ng-template #empty>
        <div class="card">
          <div class="card-content">
            <p>Giỏ hàng của bạn đang trống</p>
            <button mat-raised-button color="primary" routerLink="/product"><mat-icon>shopping_bag</mat-icon> Tiếp tục mua sắm</button>
          </div>
        </div>
      </ng-template>
    </app-page-layout>
  `,
  styles: [`
    :host {
      display: block;
    }
    
    .page-container {
      max-width: 1200px;
      margin: 0 auto;
      padding: 24px;
      background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
      min-height: calc(100vh - 64px);
    }

    .page-title {
      font-size: 2rem;
      font-weight: 600;
      color: #1976d2;
      margin-bottom: 24px;
      padding-bottom: 12px;
      border-bottom: 3px solid #1976d2;
    }

    .table-container {
      background: #fff;
      border-radius: 12px;
      box-shadow: 0 2px 12px rgba(33,150,243,0.08);
      overflow: hidden;
      margin-top: 24px;
    }

    .mat-mdc-row:hover {
      background: #f5f5f5;
    }
    
    .button-group {
      padding: 16px;
      display: flex;
      align-items: center;
      gap: 16px;
    }

    .spacer {
      flex: 1 1 auto;
    }

    .card {
      background: #fff;
      border-radius: 12px;
      box-shadow: 0 2px 12px rgba(33,150,243,0.08);
      padding: 24px;
      text-align: center;

      button {
        margin-top: 16px;
      }
    }
  `]
})
export class CartPage implements OnInit {
  cartItems: any[] = [];
  displayedColumns: string[] = ['name', 'quantity', 'price', 'actions'];
  isLoggedIn = false;
  errorMsg = '';

  constructor(private cartService: CartService, private orderService: OrderService, private router: Router, private api: Api) {}

  ngOnInit() {
    // Reactive subscription (single click load)
    this.cartService.items$.subscribe(list => {
      this.cartItems = list || [];
    });
    // Ensure initial fetch if service hasn't loaded yet
    this.cartService.getCart().subscribe();
  this.isLoggedIn = !!localStorage.getItem('jwt_token');
  }

  getTotal() {
    return this.cartItems.reduce((sum, item) => sum + item.price * item.quantity, 0);
  }

  removeItem(index: number) {
    this.cartService.removeFromCart(index).subscribe();
  }

  updateQuantity(index: number, change: number) {
    const item = this.cartItems[index];
    if (!item) return;
    const newQuantity = item.quantity + change;
    if (newQuantity <= 0) return;
    item.quantity = newQuantity;
    // Send full cart list to backend
    this.cartService.updateCart(this.cartItems).subscribe();
  }

  resolveImage(image: string) {
    if (!image) return '';
  if (image.startsWith('http')) return image;
  // use the environment.apiUrl as base and normalize via Api helper where possible
  const base = environment.apiUrl.replace(/\/api\/?$/, ''); // remove trailing '/api'
  return `${base}/uploads/${image.replace(/^\/uploads\//,'')}`;
  }

  checkout() {
    if (this.cartItems.length === 0) return;
    this.errorMsg='';
    if (!this.isLoggedIn) {
      this.router.navigate(['/login']);
      return;
    }
    const payload = { items: this.cartItems.map(i => ({ productId: i.productId || i.id, quantity: i.quantity })) };
    this.orderService.create(payload).subscribe({
      next: (res) => {
        // Clear cart locally
        this.cartItems = [];
        this.cartService.updateCart([]).subscribe();

        // Try to start VNPay payment flow automatically. If backend returns a paymentUrl
        // redirect the browser there. Otherwise fallback to orders list.
        try {
          const orderInfoId = res?.orderInfoId || res?.id || null;
          if (orderInfoId) {
            // minimal params: bankCode empty, locale 'vn', ipAddr empty
            this.orderService.createVNPayPayment(orderInfoId, '', 'vn', '').subscribe({
              next: (r) => {
                const url = r?.paymentUrl;
                if (url) {
                  console.log('[Cart] Redirecting to VNPay', url);
                  window.location.href = url;
                } else {
                  console.log('[Cart] VNPay URL not returned, navigating to orders');
                  this.router.navigate(['/orders']);
                }
              },
              error: (e) => {
                console.error('VNPay create failed', e);
                this.router.navigate(['/orders']);
              }
            });
          } else {
            this.router.navigate(['/orders']);
          }
        } catch (e) {
          console.error('Checkout post-processing failed', e);
          this.router.navigate(['/orders']);
        }
      },
      error: (err) => {
        console.error('Checkout failed', err);
        if (err.status === 401) {
          this.errorMsg = 'Vui lòng đăng nhập trước khi đặt hàng';
          this.router.navigate(['/login']);
        } else {
          this.errorMsg = err.error?.error || 'Đặt hàng thất bại';
        }
      }
    });
  }
}
