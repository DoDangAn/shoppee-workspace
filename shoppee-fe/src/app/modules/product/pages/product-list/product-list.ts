import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { Subscription } from 'rxjs';
import { MatCardModule } from '@angular/material/card';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatGridListModule } from '@angular/material/grid-list';
import { CommonModule } from '@angular/common';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { FormsModule } from '@angular/forms';
import { CurrencyPipe, NgForOf } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ProductService } from '../../../../services/product';
import { CartService } from '../../../../services/cart';
import { PageLayout } from '../../../../components/page-layout/page-layout';

@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [
    MatCardModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatToolbarModule,
    MatGridListModule,
    MatProgressSpinnerModule,
    FormsModule,
    CommonModule,
    CurrencyPipe,
    RouterLink,
    PageLayout
  ],
  template: `
    <app-page-layout title="Danh sách sản phẩm" icon="store">

      <div class="search-bar">
        <mat-form-field appearance="outline">
          <mat-label>Tìm kiếm</mat-label>
          <input matInput [(ngModel)]="searchText" (ngModelChange)="applyFilters()" placeholder="Tên sản phẩm...">
          <mat-icon matSuffix>search</mat-icon>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Danh mục</mat-label>
          <mat-select [(ngModel)]="categoryFilter" (ngModelChange)="applyFilters()">
            <mat-option value="">Tất cả</mat-option>
            <mat-option *ngFor="let category of categories" [value]="category">
              {{category}}
            </mat-option>
          </mat-select>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Giá</mat-label>
          <mat-select [(ngModel)]="priceFilter" (ngModelChange)="applyFilters()">
            <mat-option value="">Tất cả</mat-option>
            <mat-option value="asc">Thấp đến cao</mat-option>
            <mat-option value="desc">Cao đến thấp</mat-option>
          </mat-select>
        </mat-form-field>
      </div>

      <div class="card-grid" *ngIf="!loading && filteredProducts.length">
        <mat-card class="card" *ngFor="let product of filteredProducts">
          <img mat-card-image [src]="product.image" [alt]="product.name">
          <mat-card-content>
            <h3>{{product.name}}</h3>
            <p>{{product.description}}</p>
            <p class="price">{{product.price | currency:'VND'}}</p>
          </mat-card-content>
          <mat-card-actions>
            <button mat-button color="primary" [routerLink]="['/product', product.id]">
              <mat-icon>visibility</mat-icon>
              Chi tiết
            </button>
            <button mat-raised-button color="primary" (click)="addToCart(product)">
              <mat-icon>add_shopping_cart</mat-icon>
              Thêm vào giỏ
            </button>
          </mat-card-actions>
        </mat-card>
      </div>
      <div class="loading-spinner" *ngIf="loading">
        <mat-spinner diameter="50"></mat-spinner>
      </div>
      <div class="loading-spinner" *ngIf="!loading && !filteredProducts.length">
        Không có sản phẩm.
      </div>
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

    .search-bar {
      display: flex;
      gap: 16px;
      margin-bottom: 24px;

      mat-form-field {
        flex: 1;
      }
    }

    .card-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
      gap: 24px;
      margin-top: 24px;
    }

    .card {
      background: #fff;
      border-radius: 12px;
      box-shadow: 0 2px 12px rgba(33,150,243,0.08);
      transition: transform 0.2s, box-shadow 0.2s;
      overflow: hidden;

      &:hover {
        transform: translateY(-4px);
        box-shadow: 0 4px 20px rgba(33,150,243,0.12);
      }
    }

    .price {
      font-size: 1.25rem;
      font-weight: 600;
      color: #1976d2;
    }

    img {
      height: 200px;
      object-fit: cover;
    }

    mat-card-content {
      padding: 16px;
    }

    mat-card-actions {
      display: flex;
      justify-content: space-between;
      padding: 8px 16px;
      border-top: 1px solid #e0e0e0;
    }

    .loading-spinner {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 200px;
    }

    @media (max-width: 600px) {
      .search-bar {
        flex-direction: column;
      }

      .card-grid {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class ProductList implements OnInit, OnDestroy {
  products: any[] = [];
  filteredProducts: any[] = [];
  searchText: string = '';
  priceFilter: string = '';
  categoryFilter: string = '';
  statusFilter: string = '';
  categories: string[] = ['Điện thoại', 'Laptop', 'Phụ kiện', 'Thời trang'];
  loading = false;
  private navSub?: Subscription;

  constructor(
    private productService: ProductService,
    private cartService: CartService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
  this.fetchProducts();
    // subscribe to router navigation to refetch when navigating to /product
    this.navSub = this.router.events.subscribe((e: any) => {
      if (e instanceof NavigationEnd && this.router.url.includes('/product')) {
        console.log('[ProductList] NavigationEnd -> fetchProducts');
        this.fetchProducts();
      }
    });
  }

  ngOnDestroy() {
    this.navSub?.unsubscribe();
  }

  private fetchProducts() {
    this.loading = true;
    this.productService.getAll().subscribe({
      next: (data: any) => {
        // normalize API responses: backend might return an array or a wrapper { data: [...] } or { products: [...] }
        let products: any[] = [];
        if (Array.isArray(data)) {
          products = data;
        } else if (data && Array.isArray(data.data)) {
          products = data.data;
        } else if (data && Array.isArray(data.products)) {
          products = data.products;
        } else {
          products = [];
        }

        this.products = products;
        try {
          // Derive categories dynamically from data (fallback to existing list if empty)
          const cats = Array.from(new Set((this.products
            .map((p: any) => p.category)
            .filter((c: any) => !!c))));
          if (cats.length) this.categories = cats as string[];
          this.applyFilters();
          // ensure Angular updates the view immediately after data assignment
          try { this.cdr.detectChanges(); } catch (e) { }
        } catch (err) {
          console.error('[ProductList] applyFilters error', err);
          this.filteredProducts = [];
        }
        this.loading = false;
      },
      error: () => { this.loading = false; }
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

  onSearch() {
    this.applyFilters();
  }

  onFilter() {
    this.applyFilters();
  }

  applyFilters() {
    let list = [...this.products];
    if (this.searchText) {
      const key = this.searchText.toLowerCase();
      list = list.filter(p => (p.name || '').toLowerCase().includes(key));
    }
    if (this.categoryFilter) {
      list = list.filter(p => p.category === this.categoryFilter);
    }
    if (this.statusFilter) {
      list = list.filter(p => p.status === this.statusFilter);
    }
    // Price sorting (asc/desc) instead of range filtering
    if (this.priceFilter === 'asc') {
      list.sort((a,b) => (a.price||0) - (b.price||0));
    } else if (this.priceFilter === 'desc') {
      list.sort((a,b) => (b.price||0) - (a.price||0));
    }
    this.filteredProducts = list;
  }
}
