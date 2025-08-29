import { Component, OnInit, OnDestroy, ViewChild, AfterViewInit, ChangeDetectorRef } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { Subscription } from 'rxjs';
import { AdminService } from '../../../../services/admin';
import { AuthService } from '../../../../services/auth';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatDialogModule } from '@angular/material/dialog';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { ProductVariantDialog } from './product-variant-dialog';

@Component({
  selector: 'app-product-management',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatDialogModule,
    MatInputModule,
    MatFormFieldModule
  ],
  template: `
    <div class="page-container">
      <div class="page-header">
        <h1 class="page-title">Product Management</h1>
        <button mat-raised-button color="primary" (click)="openAddDialog()">
          <mat-icon>add</mat-icon>
          Add New Product
        </button>
      </div>

      <mat-card>
        <mat-card-content>
          <div class="table-container">
            <mat-form-field>
              <mat-label>Filter</mat-label>
              <input matInput (keyup)="applyFilter($event)" placeholder="Search products..." #input>
            </mat-form-field>

            <table mat-table [dataSource]="dataSource" matSort>
              <!-- ID Column -->
              <ng-container matColumnDef="id">
                <th mat-header-cell *matHeaderCellDef>ID</th>
                <td mat-cell *matCellDef="let product">
                  <div style="display:flex;gap:8px;align-items:center;">
                    <span>{{product.id}}</span>
                    <button mat-icon-button matTooltip="Copy ID" (click)="copyId(product)">
                      <mat-icon>content_copy</mat-icon>
                    </button>
                  </div>
                </td>
              </ng-container>

              <!-- Image Column -->
              <ng-container matColumnDef="image">
                <th mat-header-cell *matHeaderCellDef>Image</th>
                <td mat-cell *matCellDef="let product">
                  <img [src]="product.image" alt="Product image" class="product-image">
                </td>
              </ng-container>

              <!-- Name Column -->
              <ng-container matColumnDef="name">
                <th mat-header-cell *matHeaderCellDef mat-sort-header>Name</th>
                <td mat-cell *matCellDef="let product">{{product.name}}</td>
              </ng-container>

              <!-- Price Column -->
              <ng-container matColumnDef="price">
                <th mat-header-cell *matHeaderCellDef mat-sort-header>Price</th>
                <td mat-cell *matCellDef="let product">{{product.price | currency:'VND'}}</td>
              </ng-container>

              <!-- Category Column -->
              <ng-container matColumnDef="category">
                <th mat-header-cell *matHeaderCellDef mat-sort-header>Category</th>
                <td mat-cell *matCellDef="let product">{{product.category}}</td>
              </ng-container>

              <!-- Stock Column -->
              <ng-container matColumnDef="stock">
                <th mat-header-cell *matHeaderCellDef mat-sort-header>Stock</th>
                <td mat-cell *matCellDef="let product">{{product.stock}}</td>
              </ng-container>

              <!-- Status Column -->
              <ng-container matColumnDef="status">
                <th mat-header-cell *matHeaderCellDef mat-sort-header>Status</th>
                <td mat-cell *matCellDef="let product">
                  <span class="status-badge" [class.active]="product.status === 'Active'">
                    {{product.status}}
                  </span>
                </td>
              </ng-container>

              <!-- Actions Column -->
              <ng-container matColumnDef="actions">
                <th mat-header-cell *matHeaderCellDef>Actions</th>
                <td mat-cell *matCellDef="let product">
                  <button mat-icon-button color="primary" (click)="openEditDialog(product)">
                    <mat-icon>edit</mat-icon>
                  </button>
                  <button mat-icon-button color="warn" (click)="deleteProduct(product)">
                    <mat-icon>delete</mat-icon>
                  </button>
                </td>
              </ng-container>

              <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
              <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>

              <!-- Row shown when there is no matching data -->
              <tr class="mat-row" *matNoDataRow>
                <td class="mat-cell" colspan="7">No data matching the filter "{{input.value}}"</td>
              </tr>
            </table>

            <mat-paginator [pageSizeOptions]="[5, 10, 25, 100]" aria-label="Select page of products"></mat-paginator>
          </div>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .page-container {
      padding: 20px;
    }

    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 20px;

      .page-title {
        margin: 0;
        color: #1976d2;
      }
    }

    .table-container {
      mat-form-field {
        width: 100%;
        margin-bottom: 20px;
      }

      .mat-mdc-table {
        width: 100%;
      }

      .product-image {
        width: 50px;
        height: 50px;
        object-fit: cover;
        border-radius: 4px;
      }

      .status-badge {
        padding: 4px 8px;
        border-radius: 12px;
        font-size: 12px;
        background-color: #f5f5f5;
        color: #666;

        &.active {
          background-color: #e8f5e9;
          color: #2e7d32;
        }
      }
    }

    @media (max-width: 600px) {
      .page-header {
        flex-direction: column;
        gap: 16px;
        align-items: stretch;

        button {
          width: 100%;
        }
      }
    }
  `]
})
export class ProductManagement implements OnInit, AfterViewInit, OnDestroy {
  displayedColumns: string[] = ['id', 'image', 'name', 'price', 'category', 'stock', 'status', 'actions'];
  dataSource = new MatTableDataSource<any>([]);
  loading = false;
  error: string | null = null;
  private navSub?: Subscription;

  // make paginator/sort available earlier to avoid a timing race where data is set
  // before the ViewChildren are ready which can make the table not render on first load
  @ViewChild(MatPaginator, { static: true }) paginator!: MatPaginator;
  @ViewChild(MatSort, { static: true }) sort!: MatSort;

  constructor(
    private adminService: AdminService,
    private router: Router,
    private cdr: ChangeDetectorRef,
    private auth: AuthService,
    private snackBar: MatSnackBar
    , private dialog: MatDialog
  ) {}

  ngOnInit() {
    console.log('[ProductManagement] ngOnInit, currentUrl=', this.router.url);
    // ensure paginator/sort are wired before we load data to avoid table render races
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;

    // Only fetch admin data when the user is confirmed as admin. This avoids making
    // unauthenticated requests on first navigation which return empty/401 and force a second click.
    if (this.auth.isAdminUser()) {
      this.fetchProducts();
    } else if (this.auth.getToken()) {
      // token present but roles may not be loaded yet
      this.auth.refreshProfile().subscribe({
        next: () => {
          if (this.auth.isAdminUser()) this.fetchProducts();
          else this.router.navigateByUrl('/');
        },
        error: () => this.router.navigateByUrl('/')
      });
    } else {
      this.router.navigateByUrl('/auth/login');
    }
    this.navSub = this.router.events.subscribe((event: any) => {
      console.log('[ProductManagement] router event', event);
      if (event instanceof NavigationEnd) {
        console.log('[ProductManagement] NavigationEnd url=', this.router.url);
        if (this.router.url.includes('/admin/products')) {
          this.fetchProducts();
        }
      }
    });
  }

  ngAfterViewInit() {
  // paginator/sort were already assigned in ngOnInit (with static ViewChild) but
  // keep a safe re-assign here in case of changes to template
  this.dataSource.paginator = this.paginator;
  this.dataSource.sort = this.sort;
  }

  ngOnDestroy() {
    this.navSub?.unsubscribe();
  }

  applyFilter(event: Event) {
    const filterValue = (event.target as HTMLInputElement).value;
    this.dataSource.filter = filterValue.trim().toLowerCase();
    if (this.dataSource.paginator) {
      this.dataSource.paginator.firstPage();
    }
  }

  copyId(product: any) {
    if (!product?.id) return;
    navigator.clipboard?.writeText(product.id.toString()).then(() => {
      this.snackBar.open('Product ID copied to clipboard', 'Close', { duration: 1500 });
    }).catch(() => {
      this.snackBar.open('Failed to copy ID', 'Close', { duration: 1500 });
    });
  }

  openAddDialog() {
    const dialogRef = this.dialog.open(ProductVariantDialog, {
      width: '520px',
      data: { parents: [] }
    });

    dialogRef.afterClosed().subscribe(res => {
      if (!res) return;
      const payload: any = {
        productId: String(res.productId || res.productID || res.productId || ''),
        productName: res.productName,
        price: res.price,
        quantity: res.quantity,
        categoryId: res.categoryId || null
      };
      console.log('[ProductManagement] createProductVariant payload', payload);
      this.adminService.createProductVariant(payload).subscribe({
        next: (r: any) => {
          console.log('[ProductManagement] createProductVariant response', r);
          const catName = r?.categoryName || r?.category || null;
          const msg = catName ? `Variant created (category: ${catName})` : 'Variant created';
          this.snackBar.open(msg, 'Close', { duration: 1400 });
          this.fetchProducts();
        },
        error: (err: any) => {
          console.error('[ProductManagement] createProductVariant error', err);
          const details = err?.error?.details || err?.error || err?.message || 'Unknown error';
          this.snackBar.open('Failed to create variant: ' + details, 'Close', { duration: 4000 });
        }
      });
    });
  }

  openEditDialog(product: any) {
    const id = product.id;
    if (!id) return alert('No variant id available');
    const dialogRef = this.dialog.open(ProductVariantDialog, {
      width: '520px',
      data: { variant: product, parents: [] }
    });
    dialogRef.afterClosed().subscribe(res => {
      if (!res) return;
      const payload: any = {
        productName: res.productName,
        price: res.price,
        quantity: res.quantity,
        categoryId: res.categoryId || null
      };
      // if parent product changed, include productId
      if (res.productId || res.productID) payload.productId = String(res.productId || res.productID);
      console.log('[ProductManagement] updateProductVariant payload', id, payload);
      this.adminService.updateProductVariant(id, payload).subscribe({
  next: (r: any) => { console.log('[ProductManagement] updateProductVariant response', r); const catName = r?.categoryName || r?.category || null; const msg = catName ? `Variant updated (category: ${catName})` : 'Variant updated'; this.snackBar.open(msg, 'Close', { duration: 1400 }); this.fetchProducts(); },
        error: (e: any) => {
          console.error('[ProductManagement] updateProductVariant error', e);
          const details = e?.error?.details || e?.error || e?.message || 'Unknown error';
          this.snackBar.open('Error updating variant: ' + details, 'Close', { duration: 4000 });
        }
      });
    });
  }

  deleteProduct(product: any) {
    const id = product.id;
    if (!id) return alert('No variant id available');
    if (!confirm('Delete variant ' + (product.name || id) + '?')) return;
    this.adminService.deleteProductVariant(id).subscribe({
      next: () => this.fetchProducts(),
      error: (e) => alert('Error deleting variant: ' + (e?.error?.details || e?.message || e))
    });
  }

  private fetchProducts() {
    console.log('[ProductManagement] fetchProducts start');
    this.loading = true;
    this.adminService.getProducts().subscribe({
      next: (res: any) => {
        console.log('[ProductManagement] fetchProducts response', res);
        // backend may return an array or a wrapper like { data: [...] } or { products: [...] }
        let products: any[] = [];
        if (Array.isArray(res)) {
          products = res;
        } else if (res && Array.isArray(res.data)) {
          products = res.data;
        } else if (res && Array.isArray(res.products)) {
          products = res.products;
        } else if (res && Array.isArray(res.content)) {
          products = res.content;
        } else if (res && Array.isArray(res.products)) {
          products = res.products;
        } else {
          products = [];
        }

        const normalized = products.map((p: any) => ({
          id: p.id ?? p.productID ?? p.productId ?? null,
          name: p.productName ?? p.name ?? '',
          price: p.price ?? p.cost ?? p.totalPrice ?? 0,
          image: p.imageUrl ?? p.image ?? p.imagePath ?? p.imageUrl ?? '/uploads/default.png',
          category: (p.categories && p.categories.length) ? (p.categories[0].categoryName || p.categories[0].name) : (p.categoryName || ''),
          stock: p.quantity ?? p.stock ?? p.count ?? 0,
          status: p.status ?? (p.active ? 'Active' : 'Inactive')
        }));
  this.dataSource.data = normalized;
  // force change detection to ensure table updates immediately
  try { this.cdr.detectChanges(); } catch (e) { /* noop */ }
        this.loading = false;
        this.error = null;
      },
      error: (err: any) => {
        console.error('[ProductManagement] fetchProducts error', err);
        this.error = err?.error?.error || 'Lỗi tải sản phẩm';
        this.loading = false;
      }
    });
  }
}
