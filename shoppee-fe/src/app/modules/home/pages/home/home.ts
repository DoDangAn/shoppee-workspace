import { Component, OnInit } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { HttpClientModule } from '@angular/common/http';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatGridListModule } from '@angular/material/grid-list';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { CurrencyPipe, NgForOf } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ProductService } from '../../../../services/product';
import { CartService } from '../../../../services/cart';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatGridListModule,
    CurrencyPipe,
    MatIconModule,
    MatToolbarModule,
    MatFormFieldModule,
    MatInputModule,
    MatGridListModule,
    CommonModule,
    CurrencyPipe,
    RouterLink,
    NgForOf,
    HttpClientModule
  ],
  template: `
    <div class="page-container">
      <!-- Hero Section -->
      <section class="hero">
        <div class="hero-content">
          <h1>Chào mừng đến với Shoppee</h1>
          <p>Khám phá hàng ngàn sản phẩm với giá tốt nhất</p>
          <button mat-raised-button color="primary" routerLink="/product">
            <mat-icon>shopping_bag</mat-icon>
            Mua sắm ngay
          </button>
        </div>
      </section>

      <!-- Categories Section -->
      <section class="categories">
        <h2 class="section-title">Danh mục sản phẩm</h2>
        <div class="category-grid">
          <mat-card class="category-card" *ngFor="let category of categories" [routerLink]="['/product']" [queryParams]="{category: category.id}">
            <mat-icon class="category-icon">{{category.icon}}</mat-icon>
            <mat-card-content>
              <h3>{{category.name}}</h3>
              <p>{{category.description}}</p>
            </mat-card-content>
          </mat-card>
        </div>
      </section>

      <!-- Featured Products -->
      <section class="featured-products">
        <h2 class="section-title">Sản phẩm nổi bật</h2>
        <div class="product-grid">
          <mat-card class="product-card" *ngFor="let product of featuredProducts">
            <img mat-card-image [src]="product.image" [alt]="product.name">
            <mat-card-content>
              <h3>{{product.name}}</h3>
              <p class="price">{{product.price | currency:'VND'}}</p>
            </mat-card-content>
            <mat-card-actions>
              <button mat-button color="primary" [routerLink]="['/product', product.id]">
                Chi tiết
              </button>
              <button mat-raised-button color="primary" (click)="addToCart(product)">
                <mat-icon>add_shopping_cart</mat-icon>
                Thêm vào giỏ
              </button>
            </mat-card-actions>
          </mat-card>
        </div>
      </section>
    </div>
  `,
  styles: [`
    .page-container {
      max-width: 1200px;
      margin: 0 auto;
      padding: 24px;
    }

    .hero {
      background: linear-gradient(135deg, #1976d2 0%, #64b5f6 100%);
      color: white;
      border-radius: 12px;
      padding: 48px;
      margin-bottom: 48px;
      text-align: center;
    }

    .hero-content {
      max-width: 600px;
      margin: 0 auto;

      h1 {
        font-size: 2.5rem;
        margin-bottom: 16px;
      }

      p {
        font-size: 1.2rem;
        margin-bottom: 24px;
        opacity: 0.9;
      }

      button {
        padding: 8px 32px;
        font-size: 1.1rem;
      }
    }

    .section-title {
      font-size: 2rem;
      color: #1976d2;
      margin-bottom: 24px;
      text-align: center;
    }

    .category-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
      gap: 24px;
      margin-bottom: 48px;
    }

    .category-card {
      text-align: center;
      padding: 24px;
      cursor: pointer;
      transition: transform 0.2s;

      &:hover {
        transform: translateY(-4px);
      }

      .category-icon {
        font-size: 48px;
        height: 48px;
        width: 48px;
        color: #1976d2;
        margin-bottom: 16px;
      }

      h3 {
        margin: 0 0 8px;
        color: #1976d2;
      }
    }

    .product-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
      gap: 24px;
    }

    .product-card {
      img {
        height: 200px;
        object-fit: cover;
      }

      mat-card-content {
        padding: 16px;

        h3 {
          margin: 0 0 8px;
          font-size: 1.2rem;
        }

        .price {
          color: #1976d2;
          font-size: 1.25rem;
          font-weight: 600;
        }
      }

      mat-card-actions {
        padding: 8px 16px;
        display: flex;
        justify-content: space-between;
      }
    }

    @media (max-width: 600px) {
      .hero {
        padding: 32px 16px;

        h1 {
          font-size: 2rem;
        }
      }

      .category-grid {
        grid-template-columns: 1fr;
      }

      .product-grid {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class HomeComponent implements OnInit {
  categories = [
    { id: 'phone', name: 'Điện thoại', icon: 'smartphone', description: 'Điện thoại thông minh các loại' },
    { id: 'laptop', name: 'Laptop', icon: 'laptop', description: 'Laptop & máy tính xách tay' },
    { id: 'accessories', name: 'Phụ kiện', icon: 'headset', description: 'Phụ kiện điện tử' },
    { id: 'fashion', name: 'Thời trang', icon: 'checkroom', description: 'Quần áo & phụ kiện thời trang' }
  ];

  featuredProducts: any[] = [];

  constructor(
    private productService: ProductService,
    private cartService: CartService
  ) {}

  ngOnInit() {
    this.productService.getAll().subscribe((data: any) => {
      this.featuredProducts = data.slice(0, 8); // Lấy 8 sản phẩm đầu tiên làm sản phẩm nổi bật
    });
  }

  addToCart(product: any) {
    this.cartService.addToCart({
      productId: product.id,
      quantity: 1
    }).subscribe(() => {
      // TODO: Show success message
      console.log('Added to cart:', product);
    });
  }
}
