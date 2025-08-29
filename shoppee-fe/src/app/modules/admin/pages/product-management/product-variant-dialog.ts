import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { AdminService } from '../../../../services/admin';
import { MatIconModule } from '@angular/material/icon';
import { ChangeDetectorRef, NgZone } from '@angular/core';
import { Subject } from 'rxjs';

@Component({
  selector: 'app-product-variant-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatSelectModule, ReactiveFormsModule],
  template: `
    <h2 mat-dialog-title>{{isEdit ? 'Edit' : 'Create'}} Variant</h2>
    <mat-dialog-content>
      <form [formGroup]="form">
        <mat-form-field appearance="fill" class="full-width">
          <mat-label>Search parent product</mat-label>
          <input matInput (keyup.enter)="doSearch(searchInput.value)" #searchInput placeholder="Type a name fragment and press Enter">
        </mat-form-field>

        <mat-form-field appearance="fill" class="full-width">
          <mat-label>Parent product</mat-label>
          <mat-select formControlName="productId">
            <mat-option *ngFor="let p of parents" [value]="p.productID ?? p.id ?? p.productId">{{p.productName ?? p.name}} ({{p.productID ?? p.id}})</mat-option>
          </mat-select>
        </mat-form-field>

        <mat-form-field appearance="fill" class="full-width">
          <mat-label>Variant name</mat-label>
          <input matInput formControlName="productName" />
        </mat-form-field>

        <mat-form-field appearance="fill" class="full-width">
          <mat-label>Price</mat-label>
          <input matInput type="number" formControlName="price" />
        </mat-form-field>

        <mat-form-field appearance="fill" class="full-width">
          <mat-label>Quantity</mat-label>
          <input matInput type="number" formControlName="quantity" />
        </mat-form-field>

        <mat-form-field appearance="fill" class="full-width">
          <mat-label>Category (optional)</mat-label>
          <input matInput formControlName="categoryId" placeholder="type to search category by name" (input)="onCategoryInput($any($event.target).value)" [value]="getCategoryDisplay(form.get('categoryId').value)" />
          <mat-select (selectionChange)="onCategorySelected($event.value)">
            <mat-option *ngFor="let c of categories" [value]="c.categoryID">{{c.categoryName}} ({{c.categoryID}})</mat-option>
          </mat-select>
        </mat-form-field>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="onCancel()">Cancel</button>
      <button mat-raised-button color="primary" (click)="onSubmit()" [disabled]="form.invalid">{{isEdit ? 'Update' : 'Create'}}</button>
    </mat-dialog-actions>
  `,
  styles: [`.full-width { width: 100%; margin-bottom: 12px; }`]
})
export class ProductVariantDialog {
  form: any;
  isEdit = false;
  parents: Array<any> = [];
  categories: Array<any> = [];
  private categorySearch$ = new Subject<string>();

  constructor(
    private dialogRef: MatDialogRef<ProductVariantDialog>,
    private fb: FormBuilder,
    private adminService: AdminService,
    private cdr: ChangeDetectorRef,
    private ngZone: NgZone,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) {
    this.form = this.fb.group({
      productId: ['', Validators.required],
      productName: ['', Validators.required],
      price: [0, [Validators.required, Validators.min(0)]],
      quantity: [0, [Validators.required, Validators.min(0)]],
      categoryId: ['']
    });
    this.isEdit = !!data?.variant;
    this.parents = data?.parents || [];
    // subscribe to category search subject
    this.categorySearch$.subscribe(q => {
      if (!q || q.trim().length === 0) {
        this.categories = [];
        try { this.cdr.detectChanges(); } catch (e) {}
        return;
      }
      this.adminService.searchCategories(q.trim()).subscribe({
        next: (res: any) => {
          const list = Array.isArray(res) ? res : (res && Array.isArray(res.data) ? res.data : []);
          this.ngZone.run(() => setTimeout(() => { this.categories = list; try { this.cdr.detectChanges(); } catch (e) {} }, 0));
        },
        error: () => { this.ngZone.run(() => setTimeout(() => { this.categories = []; try { this.cdr.detectChanges(); } catch (e) {} }, 0)); }
      });
    });
    if (this.isEdit) {
      const v = data.variant;
      this.form.patchValue({
        productId: v.productId ?? v.productID ?? v.product?.id ?? '',
        productName: v.productName ?? v.name ?? '',
        price: v.price ?? 0,
        quantity: v.quantity ?? v.stock ?? 0,
        categoryId: v.categoryId ?? ''
      });
    }
  }

  doSearch(query: string) {
    if (!query || query.trim().length === 0) return;
    this.adminService.searchProducts(query.trim()).subscribe({
      next: (res: any) => {
        // expecting array of { productID, productName }
        const list = Array.isArray(res) ? res : (res && Array.isArray(res.data) ? res.data : []);
        // update parents on next tick to avoid ExpressionChangedAfterItHasBeenCheckedError
        this.ngZone.run(() => {
          setTimeout(() => {
            this.parents = list;
            try { this.cdr.detectChanges(); } catch (e) { /* noop */ }
          }, 0);
        });
      },
      error: () => {
        this.ngZone.run(() => setTimeout(() => { this.parents = []; try { this.cdr.detectChanges(); } catch (e) {} }, 0));
      }
    });
  }

  onSubmit() {
    if (this.form.valid) {
      this.dialogRef.close(this.form.value);
    }
  }

  onCancel() {
    this.dialogRef.close();
  }

  // called from template input (public)
  onCategoryInput(val: string) {
    this.categorySearch$.next(val || '');
  }

  onCategorySelected(id: any) {
    this.form.patchValue({ categoryId: id });
  }

  getCategoryDisplay(val: any) {
    if (!val) return '';
    const found = this.categories.find((c: any) => (c.categoryID === val || c.categoryId === val));
    return found ? found.categoryName : val;
  }
}
