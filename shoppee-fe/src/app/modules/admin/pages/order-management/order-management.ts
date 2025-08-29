import { Component, OnInit, ViewChild, AfterViewInit, ChangeDetectorRef } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatMenuModule } from '@angular/material/menu';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatChipsModule } from '@angular/material/chips';
import { OrderService, Order } from './order.service';
import { AuthService } from '../../../../services/auth';
import { OrderDetailsDialog } from './order-details-dialog';

@Component({
  selector: 'app-order-management',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatInputModule,
    MatFormFieldModule,
    MatSelectModule,
    MatMenuModule,
    MatDialogModule,
    MatChipsModule
  ],
  template: `
    <div class="page-container">
      <div class="page-header">
        <h1 class="page-title">Order Management</h1>
        <mat-form-field appearance="outline" class="status-filter">
          <mat-label>Filter by Status</mat-label>
          <mat-select (selectionChange)="filterByStatus($event.value)">
            <mat-option value="all">All Orders</mat-option>
            <mat-option value="PENDING">Pending</mat-option>
            <mat-option value="PROCESSING">Processing</mat-option>
            <mat-option value="SHIPPED">Shipped</mat-option>
            <mat-option value="DELIVERED">Delivered</mat-option>
            <mat-option value="CANCELLED">Cancelled</mat-option>
          </mat-select>
        </mat-form-field>
      </div>

      <mat-card>
        <mat-card-content>
          <div class="table-container">
            <mat-form-field>
              <mat-label>Filter</mat-label>
              <input matInput (keyup)="applyFilter($event)" placeholder="Search orders..." #input>
            </mat-form-field>

            <table mat-table [dataSource]="dataSource" matSort>
              <!-- Order Number Column -->
              <ng-container matColumnDef="orderNumber">
                <th mat-header-cell *matHeaderCellDef mat-sort-header>Order #</th>
                <td mat-cell *matCellDef="let order">{{order.orderNumber}}</td>
              </ng-container>

              <!-- Customer Column -->
              <ng-container matColumnDef="customer">
                <th mat-header-cell *matHeaderCellDef mat-sort-header>Customer</th>
                <td mat-cell *matCellDef="let order">{{order.userName}}</td>
              </ng-container>

              <!-- Total Amount Column -->
              <ng-container matColumnDef="totalAmount">
                <th mat-header-cell *matHeaderCellDef mat-sort-header>Total</th>
                <td mat-cell *matCellDef="let order">{{order.totalAmount | currency:'VND':'symbol':'1.0-0'}}</td>
              </ng-container>

              <!-- Status Column -->
              <ng-container matColumnDef="status">
                <th mat-header-cell *matHeaderCellDef mat-sort-header>Status</th>
                <td mat-cell *matCellDef="let order">
                  <mat-chip [class]="'status-' + order.status.toLowerCase()">
                    {{order.status}}
                  </mat-chip>
                </td>
              </ng-container>

              <!-- Date Column -->
              <ng-container matColumnDef="date">
                <th mat-header-cell *matHeaderCellDef mat-sort-header>Date</th>
                <td mat-cell *matCellDef="let order">{{order.createdAt | date:'medium'}}</td>
              </ng-container>

              <!-- Actions Column -->
              <ng-container matColumnDef="actions">
                <th mat-header-cell *matHeaderCellDef>Actions</th>
                <td mat-cell *matCellDef="let order">
                  <button mat-icon-button [matMenuTriggerFor]="menu">
                    <mat-icon>more_vert</mat-icon>
                  </button>
                  <mat-menu #menu="matMenu">
                    <button mat-menu-item (click)="viewOrderDetails(order)">
                      <mat-icon>visibility</mat-icon>
                      <span>View Details</span>
                    </button>
                    <button mat-menu-item (click)="updateStatus(order, 'PROCESSING')" 
                            *ngIf="order.status === 'PENDING'">
                      <mat-icon>local_shipping</mat-icon>
                      <span>Process Order</span>
                    </button>
                    <button mat-menu-item (click)="updateStatus(order, 'SHIPPED')"
                            *ngIf="order.status === 'PROCESSING'">
                      <mat-icon>local_shipping</mat-icon>
                      <span>Mark as Shipped</span>
                    </button>
                    <button mat-menu-item (click)="updateStatus(order, 'DELIVERED')"
                            *ngIf="order.status === 'SHIPPED'">
                      <mat-icon>check_circle</mat-icon>
                      <span>Mark as Delivered</span>
                    </button>
                    <button mat-menu-item (click)="updateStatus(order, 'CANCELLED')"
                            *ngIf="['PENDING', 'PROCESSING'].includes(order.status)">
                      <mat-icon color="warn">cancel</mat-icon>
                      <span class="text-warn">Cancel Order</span>
                    </button>
                  </mat-menu>
                  <!-- temporary inline test button to verify click handler (visible for PENDING rows) -->
                  <button mat-mini-button color="primary" *ngIf="order.status === 'PENDING'" (click)="updateStatus(order, 'PROCESSING')" style="margin-left:8px;">
                    Process
                  </button>
                </td>
              </ng-container>

              <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
              <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>

              <!-- Row shown when there is no matching data -->
              <tr class="mat-row" *matNoDataRow>
                <td class="mat-cell" colspan="6">No data matching the filter "{{input.value}}"</td>
              </tr>
            </table>

            <mat-paginator [pageSizeOptions]="[5, 10, 25, 100]" aria-label="Select page of orders"></mat-paginator>
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

      .status-filter {
        width: 200px;
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
    }

    .text-warn {
      color: #f44336;
    }

    .status-pending { background-color: #fff3e0; color: #e65100; }
    .status-processing { background-color: #e3f2fd; color: #1565c0; }
    .status-shipped { background-color: #e8f5e9; color: #2e7d32; }
    .status-delivered { background-color: #e8eaf6; color: #283593; }
    .status-cancelled { background-color: #fbe9e7; color: #c62828; }

    @media (max-width: 600px) {
      .page-header {
        flex-direction: column;
        gap: 16px;
        align-items: stretch;

        .status-filter {
          width: 100%;
        }
      }
    }
  `]
})
export class OrderManagement implements OnInit, AfterViewInit {
  displayedColumns: string[] = ['orderNumber', 'customer', 'totalAmount', 'status', 'date', 'actions'];
  dataSource: MatTableDataSource<Order>;
  originalData: Order[] = [];

  @ViewChild(MatPaginator, { static: true }) paginator!: MatPaginator;
  @ViewChild(MatSort, { static: true }) sort!: MatSort;

  constructor(
    private orderService: OrderService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef,
    private auth: AuthService,
    private router: Router
  ) {
    this.dataSource = new MatTableDataSource<Order>([]);
  }

  ngOnInit() {
    // wire paginator/sort early to avoid render race
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;

    console.log('[OrderManagement] ngOnInit - isAdminUser=', this.auth.isAdminUser(), 'hasToken=', !!this.auth.getToken());
  console.log('[OrderManagement] ngOnInit - loading orders');
  this.loadOrders();
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

  filterByStatus(status: string) {
    if (status === 'all') {
      this.dataSource.data = this.originalData;
    } else {
      this.dataSource.data = this.originalData.filter(order => order.status === status);
    }
  }

  viewOrderDetails(order: Order) {
    this.dialog.open(OrderDetailsDialog, {
      data: order,
      width: '600px'
    });
  }

  updateStatus(order: Order, newStatus: Order['status']) {
    const statusMessages: Record<Order['status'], string> = {
      'PENDING': 'Order is pending',
      'PROCESSING': 'Order is now being processed',
      'SHIPPED': 'Order has been shipped',
      'DELIVERED': 'Order has been delivered',
      'CANCELLED': 'Order has been cancelled'
    };

  console.log('[OrderManagement] updateStatus called', { id: order.id, newStatus, order });
    this.orderService.updateOrderStatus(order.id, newStatus).subscribe({
      next: (res) => {
        console.log('[OrderManagement] updateStatus success', res);
        this.loadOrders();
        this.showMessage(statusMessages[newStatus]);
      },
      error: (error) => {
        this.showMessage('Error updating order status', true);
        console.error('[OrderManagement] Error updating order status:', error);
      }
    });
  }

  private loadOrders() {
    this.orderService.getOrders().subscribe({
      next: (res: any) => {
  // log raw response to help debug missing fields
  console.log('[OrderManagement] raw response', res);
        const rawList = Array.isArray(res) ? res : (Array.isArray(res?.content) ? res.content : []);
        // normalize fields so template can display values even if backend shape differs
        const list = (rawList || []).map((o: any) => ({
          id: o.id ?? o.orderId ?? null,
          orderNumber: o.orderNumber ?? o.orderNo ?? o.id ?? o.orderId ?? '',
          userId: o.userId ?? o.customerId ?? o.user?.id ?? null,
          userName: o.userName ?? o.username ?? o.user?.fullName ?? o.customerName ?? o.fullname ?? '',
          status: o.status ?? o.state ?? 'PENDING',
          totalAmount: o.totalAmount ?? o.total ?? o.totalPrice ?? o.amount ?? 0,
          items: o.items ?? o.orderItems ?? o.products ?? [],
          shippingAddress: o.shippingAddress ?? o.address ?? '',
          paymentMethod: o.paymentMethod ?? o.payment ?? '',
          createdAt: o.createdAt ? new Date(o.createdAt) : (o.createdDate ? new Date(o.createdDate) : new Date()),
          updatedAt: o.updatedAt ? new Date(o.updatedAt) : (o.updatedDate ? new Date(o.updatedDate) : new Date())
        } as Order));
  console.log('[OrderManagement] loadOrders - received normalized', list.length);
        this.originalData = list;
        this.dataSource.data = list;
        try { this.cdr.detectChanges(); } catch (e) { }
      },
      error: (error) => {
        this.showMessage('Error loading orders', true);
        console.error('[OrderManagement] loadOrders error', error);
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
