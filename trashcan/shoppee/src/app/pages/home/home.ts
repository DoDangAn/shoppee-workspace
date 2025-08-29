import { Component, OnInit } from '@angular/core';
import { CommonModule, CurrencyPipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Router } from '@angular/router';
import { HomeService } from '../../services/home';
import { HomeResponse } from '../../models/home.model';
import { ProductVariant } from '../../models/product.model';

@Component({
  selector: 'app-home',
  templateUrl: './home.html',
  styleUrls: ['./home.css'],
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  providers: [CurrencyPipe]
})
export class Home implements OnInit {
  homeData?: HomeResponse;
  productVariants: ProductVariant[] = [];
  loading = true;
  error: string | null = null;

  constructor(
    private homeService: HomeService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadHomeData();
  }

  loadHomeData(): void {
    this.loading = true;
    this.homeService.getHomeData('viewCount', 'score', 'releaseDate')
      .subscribe({
        next: (data: HomeResponse) => {
          this.homeData = data;
          // Nếu BE trả về products là mảng sản phẩm có productVariants
          if (Array.isArray(data.products)) {
            this.productVariants = data.products
              .map((product: any) => Array.isArray(product.productVariants) ? product.productVariants : [])
              .flat();
          } 
          // Nếu BE trả về trực tiếp mảng biến thể
          else if (Array.isArray((data as any).productVariants)) {
            this.productVariants = (data as any).productVariants;
          } else {
            this.productVariants = [];
          }
          this.loading = false;
        },
        error: (error: any) => {
          this.error = 'Failed to load home data';
          this.loading = false;
          console.error('Error loading home data:', error);
        }
      });
  }

  navigateToProduct(variantId: number): void {
    this.router.navigate(['/product-variant', variantId]);
  }
}