import { Component, OnInit, ViewChild, AfterViewInit, ChangeDetectorRef } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../../services/auth';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CategoryDialog } from './category-dialog/category-dialog';
import { CategoryService, Category } from './category.service';

@Component({
  selector: 'app-category-management',
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
        <h1 class="page-title">Category Management</h1>
        <button mat-raised-button color="primary" (click)="openAddDialog()">
          <mat-icon>add</mat-icon>
          Add New Category
        </button>
      </div>

      <mat-card>
        <mat-card-content>
          <div class="table-container">
            <mat-form-field>
              <mat-label>Filter</mat-label>
              <input matInput (keyup)="applyFilter($event)" placeholder="Search categories..." #input>
            </mat-form-field>

            <table mat-table [dataSource]="dataSource" matSort>
              <!-- Name Column -->
              <ng-container matColumnDef="name">
                <th mat-header-cell *matHeaderCellDef mat-sort-header>Name</th>
                <td mat-cell *matCellDef="let category">{{category.name}}</td>
              </ng-container>

              <!-- Description Column -->
              <ng-container matColumnDef="description">
                <th mat-header-cell *matHeaderCellDef>Description</th>
                <td mat-cell *matCellDef="let category">{{category.description}}</td>
              </ng-container>

              <!-- Products Count Column -->
              <ng-container matColumnDef="productsCount">
                <th mat-header-cell *matHeaderCellDef mat-sort-header>Products</th>
                <td mat-cell *matCellDef="let category">{{category.productsCount}}</td>
              </ng-container>

              <!-- Status Column -->
              <ng-container matColumnDef="status">
                <th mat-header-cell *matHeaderCellDef mat-sort-header>Status</th>
                <td mat-cell *matCellDef="let category">
                  <span class="status-badge" [class.active]="category.status === 'Active'">
                    {{category.status}}
                  </span>
                </td>
              </ng-container>

              <!-- Actions Column -->
              <ng-container matColumnDef="actions">
                <th mat-header-cell *matHeaderCellDef>Actions</th>
                <td mat-cell *matCellDef="let category">
                  <button mat-icon-button color="primary" (click)="openEditDialog(category)">
                    <mat-icon>edit</mat-icon>
                  </button>
                  <button mat-icon-button color="warn" (click)="deleteCategory(category)">
                    <mat-icon>delete</mat-icon>
                  </button>
                </td>
              </ng-container>

              <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
              <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>

              <!-- Row shown when there is no matching data -->
              <tr class="mat-row" *matNoDataRow>
                <td class="mat-cell" colspan="5">No data matching the filter "{{input.value}}"</td>
              </tr>
            </table>

            <mat-paginator [pageSizeOptions]="[5, 10, 25, 100]" aria-label="Select page of categories"></mat-paginator>
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
export class CategoryManagement implements OnInit, AfterViewInit {
  displayedColumns: string[] = ['name', 'description', 'productsCount', 'status', 'actions'];
  dataSource: MatTableDataSource<Category>;
  loading = false;
  error: string | null = null;

  @ViewChild(MatPaginator, { static: true }) paginator!: MatPaginator;
  @ViewChild(MatSort, { static: true }) sort!: MatSort;

  constructor(
    private categoryService: CategoryService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef,
    private auth: AuthService,
    private router: Router
  ) {
    this.dataSource = new MatTableDataSource<Category>([]);
  }

  ngOnInit() {
    // assign paginator/sort early
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;
    console.log('[CategoryManagement] ngOnInit');
    if (this.auth.isAdminUser()) {
      this.loadCategories();
    } else if (this.auth.getToken()) {
      this.auth.refreshProfile().subscribe({
        next: () => { if (this.auth.isAdminUser()) this.loadCategories(); else this.router.navigateByUrl('/'); },
        error: () => this.router.navigateByUrl('/')
      });
    } else {
      this.router.navigateByUrl('/auth/login');
    }
  }

  ngAfterViewInit() {
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;
  }

  applyFilter(event: Event) {
    const filterValue = (event.target as HTMLInputElement).value;
    this.dataSource.filter = filterValue.trim().toLowerCase();

    if (this.dataSource.paginator) {
      this.dataSource.paginator.firstPage();
    }
  }

  openAddDialog() {
    const dialogRef = this.dialog.open(CategoryDialog);

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        const payload = { categoryName: result.name, description: result.description };
        console.log('[CategoryManagement] createCategory payload', payload);
        this.categoryService.createCategory(result).subscribe({
          next: (newCategory: any) => {
            console.log('[CategoryManagement] createCategory response', newCategory);
            this.loadCategories();
            const name = newCategory?.categoryName || newCategory?.name || result.name;
            this.showMessage(`Category created: ${name}`);
          },
          error: (error) => {
            console.error('[CategoryManagement] createCategory error', error);
            const details = error?.error?.details || error?.error || error?.message || 'Unknown error';
            this.showMessage('Error creating category: ' + details, true);
          }
        });
      }
    });
  }

  openEditDialog(category: Category) {
    const dialogRef = this.dialog.open(CategoryDialog, {
      data: category
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
          const payload = { categoryName: result.name, description: result.description };
          console.log('[CategoryManagement] updateCategory payload', category.id, payload);
          this.categoryService.updateCategory(category.id as unknown as number, result).subscribe({
            next: (r: any) => { console.log('[CategoryManagement] updateCategory response', r); this.loadCategories(); const name = r?.categoryName || r?.name || result.name; this.showMessage(`Category updated: ${name}`); },
            error: (error) => {
              console.error('[CategoryManagement] updateCategory error', error);
              const details = error?.error?.details || error?.error || error?.message || 'Unknown error';
              this.showMessage('Error updating category: ' + details, true);
            }
          });
        }
    });
  }

  deleteCategory(category: Category) {
    if (confirm('Are you sure you want to delete this category?')) {
  this.categoryService.deleteCategory(category.id as unknown as number).subscribe({
        next: (r: any) => { console.log('[CategoryManagement] deleteCategory response', r); this.loadCategories(); this.showMessage('Category deleted successfully'); },
        error: (error) => {
          console.error('[CategoryManagement] deleteCategory error', error);
          const details = error?.error?.details || error?.error || error?.message || 'Unknown error';
          this.showMessage('Error deleting category: ' + details, true);
        }
      });
    }
  }

  private loadCategories() {
    this.loading = true;
    this.categoryService.getCategories().subscribe({
      next: (categories: any) => {
        console.log('[CategoryManagement] raw response', categories);
        const anyResp: any = categories as any;
        const list = Array.isArray(categories) ? categories : (anyResp && Array.isArray(anyResp.content) ? anyResp.content : []);
        const normalized = (list || []).map((c: any) => ({
          id: c.categoryID ?? c.id ?? c.categoryId ?? null,
          name: c.categoryName ?? c.name ?? c.title ?? '',
          description: c.description ?? c.desc ?? '',
          status: c.status ?? (c.active ? 'Active' : 'Inactive'),
          productsCount: c.productsCount ?? c.count ?? (Array.isArray(c.products) ? c.products.length : (c.products ? c.products.size || 0 : 0))
        } as Category));
        console.log('[CategoryManagement] loadCategories - normalized', normalized.length);
        this.dataSource.data = normalized;
        this.loading = false;
        this.error = null;
        try { this.cdr.detectChanges(); } catch (e) { }
      },
      error: (error) => {
        this.loading = false;
        this.error = error?.error?.error || error?.message || 'Lỗi tải categories';
        console.error('[CategoryManagement] loadCategories error', error);
        try { this.cdr.detectChanges(); } catch (e) { }
      }
    });
  }

  private showMessage(message: string, isError: boolean = false) {
    this.snackBar.open(message, 'Close', {
      duration: 3000,
      panelClass: isError ? ['error-snackbar'] : ['success-snackbar']
    });
  }
}
