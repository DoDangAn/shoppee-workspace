import { Component, OnInit } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { OrderService } from '../../services/order.service';

@Component({
  selector: 'app-order-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    MatIconModule,
    MatButtonModule,
    MatCardModule,
    MatButtonToggleModule,
    DatePipe,
  ],
  template: `
    <div class="page-container">
      <h2 class="page-title">
        <mat-icon>receipt_long</mat-icon>
        Đơn hàng của tôi
      </h2>

      <div class="order-filters">
        <mat-button-toggle-group [(ngModel)]="statusFilter">
          <mat-button-toggle value="">Tất cả</mat-button-toggle>
          <mat-button-toggle value="pending">Chờ xử lý</mat-button-toggle>
          <mat-button-toggle value="processing">Đang xử lý</mat-button-toggle>
          <mat-button-toggle value="shipping">Đang giao</mat-button-toggle>
          <mat-button-toggle value="completed">Hoàn thành</mat-button-toggle>
          <mat-button-toggle value="cancelled">Đã hủy</mat-button-toggle>
        </mat-button-toggle-group>
      </div>

      <div class="order-list" *ngIf="filteredOrders.length > 0">
        <mat-card class="order-card" *ngFor="let order of filteredOrders">
          <mat-card-header>
            <mat-card-title>
              Đơn hàng #{{order.id}}
              <span class="order-status" [class]="order.status.toLowerCase()">
                {{getStatusText(order.status)}}
              </span>
            </mat-card-title>
            <mat-card-subtitle>
              Ngày đặt: {{order.date | date:'dd/MM/yyyy HH:mm'}}
            </mat-card-subtitle>
          </mat-card-header>

          <mat-card-content>
            <div class="order-items">
              <div class="order-item" *ngFor="let item of order.items">
                <div class="item-details">
                  <h4>{{item.name}}</h4>
                  <p>Số lượng: {{item.quantity}}</p>
                  <p>Đơn giá: {{item.price | currency:'VND'}}</p>
                </div>
              </div>
            </div>
            
            <div class="order-summary">
              <p><strong>Tổng tiền:</strong> {{order.total | currency:'VND'}}</p>
              <p><strong>Phương thức thanh toán:</strong> {{order.paymentMethod}}</p>
              <p><strong>Địa chỉ giao hàng:</strong> {{order.shippingAddress}}</p>
            </div>
          </mat-card-content>

          <mat-card-actions>
            <button mat-button color="primary" [routerLink]="['/order', order.id]">
              <mat-icon>visibility</mat-icon>
              Chi tiết
            </button>
            <button mat-button color="warn" *ngIf="canCancel(order)" (click)="cancelOrder(order)">
              <mat-icon>cancel</mat-icon>
              Hủy đơn
            </button>
            <button mat-button color="accent" *ngIf="canReorder(order)" (click)="reorder(order)">
              <mat-icon>replay</mat-icon>
              Đặt lại
            </button>
          </mat-card-actions>
        </mat-card>
      </div>

      <div class="empty-state" *ngIf="filteredOrders.length === 0">
        <mat-icon>receipt_long</mat-icon>
        <h3>Chưa có đơn hàng nào</h3>
        <p>Hãy mua sắm để có những trải nghiệm tuyệt vời</p>
        <button mat-raised-button color="primary" routerLink="/product">
          <mat-icon>shopping_bag</mat-icon>
          Mua sắm ngay
        </button>
      </div>
    </div>
  `,
  styles: [`
    .page-container {
      max-width: 1200px;
      margin: 0 auto;
      padding: 24px;
    }

    .page-title {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 2rem;
      color: #1976d2;
      margin-bottom: 24px;
      padding-bottom: 12px;
      border-bottom: 3px solid #1976d2;

      mat-icon {
        font-size: 2rem;
        height: 2rem;
        width: 2rem;
      }
    }

    .order-filters {
      margin-bottom: 24px;
      overflow-x: auto;

      mat-button-toggle-group {
        background: white;
        border-radius: 8px;
      }
    }

    .order-list {
      display: grid;
      gap: 24px;
    }

    .order-card {
      background: white;
      border-radius: 12px;
      overflow: hidden;

      mat-card-title {
        display: flex;
        align-items: center;
        justify-content: space-between;
      }

      .order-status {
        font-size: 0.9rem;
        padding: 4px 12px;
        border-radius: 16px;
        
        &.pending { background: #fff3e0; color: #e65100; }
        &.processing { background: #e3f2fd; color: #1565c0; }
        &.shipping { background: #e8f5e9; color: #2e7d32; }
        &.completed { background: #e8f5e9; color: #2e7d32; }
        &.cancelled { background: #ffebee; color: #c62828; }
      }
    }

    .order-items {
      margin: 16px 0;
      
      .order-item {
        display: flex;
        gap: 16px;
        padding: 16px;
        border-bottom: 1px solid #eee;

        img {
          width: 80px;
          height: 80px;
          object-fit: cover;
          border-radius: 8px;
        }

        .item-details {
          h4 {
            margin: 0 0 8px;
          }

          p {
            margin: 4px 0;
            color: #666;
          }
        }
      }
    }

    .order-summary {
      background: #f5f5f5;
      padding: 16px;
      border-radius: 8px;
      margin-top: 16px;

      p {
        margin: 8px 0;
      }
    }

    .empty-state {
      text-align: center;
      padding: 48px;
      background: white;
      border-radius: 12px;

      mat-icon {
        font-size: 64px;
        height: 64px;
        width: 64px;
        color: #1976d2;
        margin-bottom: 16px;
      }

      h3 {
        margin: 0 0 8px;
        color: #1976d2;
      }

      p {
        color: #666;
        margin-bottom: 24px;
      }
    }

    @media (max-width: 600px) {
      .order-filters {
        margin: 0 -24px 24px;
        padding: 0 24px;
      }

      .order-card {
        mat-card-title {
          flex-direction: column;
          align-items: flex-start;
          gap: 8px;
        }
      }
    }
  `]
})
export class OrderListPage implements OnInit {
  statusFilter = '';
  orders: any[] = [];

  get filteredOrders() {
    if (!this.statusFilter) return this.orders;
    return this.orders.filter(order => order.status.toLowerCase() === this.statusFilter);
  }

  constructor(private orderService: OrderService) {}

  ngOnInit() {
    this.orderService.getAll().subscribe((data: any) => {
      this.orders = data;
    });
  }

  getStatusText(status: string): string {
    const statusMap: { [key: string]: string } = {
      'pending': 'Chờ xử lý',
      'processing': 'Đang xử lý',
      'shipping': 'Đang giao',
      'completed': 'Hoàn thành',
      'cancelled': 'Đã hủy'
    };
    return statusMap[status.toLowerCase()] || status;
  }

  canCancel(order: any): boolean {
    return ['pending', 'processing'].includes(order.status.toLowerCase());
  }

  canReorder(order: any): boolean {
    return ['completed', 'cancelled'].includes(order.status.toLowerCase());
  }

  cancelOrder(order: any) {
    if (confirm('Bạn có chắc chắn muốn hủy đơn hàng này?')) {
      this.orderService.cancelOrder(order.id).subscribe(() => {
        order.status = 'cancelled';
      });
    }
  }

  reorder(order: any) {
    // TODO: Implement reorder functionality
    console.log('Reorder:', order);
  }
}
