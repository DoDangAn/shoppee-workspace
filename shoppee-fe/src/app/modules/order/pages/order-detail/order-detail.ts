import { Component, OnInit } from '@angular/core';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { CurrencyPipe, NgForOf } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CommonModule, DatePipe } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { OrderService } from '../../../../services/order';

@Component({
  selector: 'app-order-detail',
  standalone: true,
  imports: [
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatProgressSpinnerModule,
    CommonModule,
    DatePipe,
    CurrencyPipe
  ],
  templateUrl: './order-detail.html',
})
export class OrderDetailPage implements OnInit {
  order: any;
  displayedColumns: string[] = ['name', 'quantity', 'price'];

  constructor(private route: ActivatedRoute, private orderService: OrderService) {}

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.orderService.getById(id).subscribe((data: any) => {
        this.order = data;
      });
    }
  }
}
