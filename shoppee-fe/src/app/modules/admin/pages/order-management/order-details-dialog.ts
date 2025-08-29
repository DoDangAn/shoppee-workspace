import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatListModule } from '@angular/material/list';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { Order } from './order.service';

@Component({
  selector: 'app-order-details-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatListModule,
    MatDividerModule,
    MatIconModule
  ],
  template: `
    <h2 mat-dialog-title>Order Details - #{{order.orderNumber}}</h2>
    <mat-dialog-content>
      <div class="order-info">
        <h3>Customer Information</h3>
        <p><strong>Name:</strong> {{order.userName}}</p>
        <p><strong>Shipping Address:</strong> {{order.shippingAddress}}</p>
        <p><strong>Payment Method:</strong> {{order.paymentMethod}}</p>
        <p><strong>Order Date:</strong> {{order.createdAt | date:'medium'}}</p>
        <p><strong>Status:</strong> {{order.status}}</p>

        <mat-divider class="my-3"></mat-divider>

        <h3>Order Items</h3>
        <mat-list>
          <mat-list-item *ngFor="let item of order.items">
            <div class="item-details">
              <span class="item-name">{{item.productName}}</span>
              <span class="item-quantity">x{{item.quantity}}</span>
              <span class="item-price">{{item.price | currency:'VND':'symbol':'1.0-0'}}</span>
              <span class="item-total">{{item.total | currency:'VND':'symbol':'1.0-0'}}</span>
            </div>
          </mat-list-item>
        </mat-list>

        <mat-divider class="my-3"></mat-divider>

        <div class="order-total">
          <h3>Order Total</h3>
          <p class="total-amount">{{order.totalAmount | currency:'VND':'symbol':'1.0-0'}}</p>
        </div>
      </div>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="close()">Close</button>
    </mat-dialog-actions>
  `,
  styles: [`
    .order-info {
      padding: 0 16px;
    }

    .my-3 {
      margin: 24px 0;
    }

    .item-details {
      display: grid;
      grid-template-columns: 2fr 1fr 1fr 1fr;
      gap: 16px;
      width: 100%;
      align-items: center;

      .item-name {
        font-weight: 500;
      }

      .item-quantity {
        color: #666;
      }

      .item-price, .item-total {
        text-align: right;
      }

      .item-total {
        font-weight: 500;
      }
    }

    .order-total {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .total-amount {
        font-size: 1.25rem;
        font-weight: 500;
        color: #1976d2;
      }
    }

    @media (max-width: 600px) {
      .item-details {
        grid-template-columns: 1fr 1fr;
        gap: 8px;

        .item-name {
          grid-column: 1 / -1;
        }
      }
    }
  `]
})
export class OrderDetailsDialog {
  constructor(
    public dialogRef: MatDialogRef<OrderDetailsDialog>,
    @Inject(MAT_DIALOG_DATA) public order: Order
  ) {}

  close(): void {
    this.dialogRef.close();
  }
}
