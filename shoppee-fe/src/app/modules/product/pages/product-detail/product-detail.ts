import { Component, OnInit } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CommonModule, DatePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { CurrencyPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ActivatedRoute } from '@angular/router';
import { ProductService } from '../../../../services/product';

@Component({
  selector: 'app-product-detail',
  standalone: true,
  imports: [
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    CommonModule,
    CurrencyPipe
  ],
  templateUrl: './product-detail.html',
  styleUrls: ['./product-detail.scss']
})
export class ProductDetail implements OnInit {
  product: any;

  constructor(private route: ActivatedRoute, private productService: ProductService) {}

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.productService.getById(id).subscribe((data: any) => {
        this.product = data;
      });
    }
  }
}
