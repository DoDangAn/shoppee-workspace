import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

@Component({
  selector: 'app-category-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    ReactiveFormsModule
  ],
  template: `
    <h2 mat-dialog-title>{{isEdit ? 'Edit' : 'Add'}} Category</h2>
    <mat-dialog-content>
      <form [formGroup]="categoryForm">
        <mat-form-field appearance="fill" class="form-field">
          <mat-label>Name</mat-label>
          <input matInput formControlName="name" placeholder="Enter category name">
          <mat-error *ngIf="categoryForm.get('name')?.hasError('required')">
            Name is required
          </mat-error>
        </mat-form-field>

        <mat-form-field appearance="fill" class="form-field">
          <mat-label>Description</mat-label>
          <textarea matInput formControlName="description" placeholder="Enter category description" rows="3"></textarea>
        </mat-form-field>

        <mat-form-field appearance="fill" class="form-field">
          <mat-label>Status</mat-label>
          <mat-select formControlName="status">
            <mat-option value="Active">Active</mat-option>
            <mat-option value="Inactive">Inactive</mat-option>
          </mat-select>
        </mat-form-field>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="onCancel()">Cancel</button>
      <button mat-raised-button color="primary" (click)="onSubmit()" [disabled]="categoryForm.invalid">
        {{isEdit ? 'Update' : 'Create'}}
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .form-field {
      width: 100%;
      margin-bottom: 16px;
    }

    mat-dialog-content {
      min-width: 400px;
      max-width: 800px;
    }

    @media (max-width: 600px) {
      mat-dialog-content {
        min-width: auto;
      }
    }
  `]
})
export class CategoryDialog {
  categoryForm: FormGroup;
  isEdit: boolean = false;

  constructor(
    private dialogRef: MatDialogRef<CategoryDialog>,
    private fb: FormBuilder,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) {
    this.isEdit = !!data;
    this.categoryForm = this.fb.group({
      name: [data?.name || '', Validators.required],
      description: [data?.description || ''],
      status: [data?.status || 'Active']
    });
  }

  onSubmit() {
    if (this.categoryForm.valid) {
      const formValue = this.categoryForm.value;
      this.dialogRef.close(formValue);
    }
  }

  onCancel() {
    this.dialogRef.close();
  }
}
