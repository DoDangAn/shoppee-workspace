import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-payment-result',
  standalone: true,
  imports: [CommonModule, RouterModule, MatCardModule, MatButtonModule],
  template: `
    <div class="page">
      <mat-card>
        <h2>Kết quả thanh toán</h2>
        <p *ngIf="message">{{ message }}</p>
        <p *ngIf="orderId"><strong>Mã đơn hàng:</strong> {{ orderId }}</p>
        <div class="actions">
          <button mat-raised-button color="primary" routerLink="/orders">Xem đơn hàng</button>
        </div>
      </mat-card>
    </div>
  `,
  styles: [`.page { display:flex; justify-content:center; padding:24px } mat-card{width:640px;padding:24px}`]
})
export class PaymentResultPage {
  message = '';
  orderId: string | null = null;

  constructor(private route: ActivatedRoute) {
    this.route.queryParamMap.subscribe(m => {
      this.message = m.get('message') || m.get('msg') || '';
      this.orderId = m.get('orderId');
    });
  }
}
