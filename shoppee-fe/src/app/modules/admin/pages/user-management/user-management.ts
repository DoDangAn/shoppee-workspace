import { Component, OnInit, ChangeDetectorRef, ViewChild, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../../services/auth';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { FormsModule } from '@angular/forms';
import { NgForOf } from '@angular/common';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { AdminService } from '../../../../services/admin';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { UserService } from './user.service';
import { MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';

@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [
    MatTableModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
  MatCardModule,
  MatPaginatorModule,
  MatSortModule,
    CommonModule,
    FormsModule
  ],
  templateUrl: './user-management.html',
  styleUrls: ['./user-management.scss']
})
export class UserManagementPage implements OnInit, OnDestroy {
  displayedColumns: string[] = ['fullname', 'email', 'roles', 'actions'];
  dataSource = new MatTableDataSource<any>([]);
  loading = false;
  error: string | null = null;

  @ViewChild(MatPaginator, { static: true }) paginator!: MatPaginator;
  @ViewChild(MatSort, { static: true }) sort!: MatSort;
  private navSub: any;

  constructor(
    private adminService: AdminService,
    private userService: UserService,
    private cdr: ChangeDetectorRef,
    private auth: AuthService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit() {
    // wire paginator/sort early
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;
    console.log('[UserManagement] ngOnInit');
    if (this.auth.isAdminUser()) {
      this.loadUsers();
    } else if (this.auth.getToken()) {
      this.auth.refreshProfile().subscribe({ next: () => { if (this.auth.isAdminUser()) this.loadUsers(); else this.router.navigateByUrl('/'); }, error: () => this.router.navigateByUrl('/') });
    } else {
      this.router.navigateByUrl('/auth/login');
    }

    this.navSub = this.router.events.subscribe((event: any) => {
      if (event.constructor && event.constructor.name === 'NavigationEnd') {
        if (this.router.url.includes('/admin/users')) this.loadUsers();
      }
    });
  }

  loadUsers() {
    this.loading = true;
    this.adminService.getUsers().subscribe({
      next: (data: any) => {
        const list = Array.isArray(data) ? data : (Array.isArray(data?.content) ? data.content : []);
        console.log('[UserManagement] loadUsers - received', list.length);
        const mapped = (list || []).map((u: any) => ({
          id: u.id,
          fullname: u.fullname || u.username || u.fullName || u.fullname,
          email: u.email,
          roles: (u.roles || []).map((r: any) => r.name || r.authority || r).join(', ')
        }));
        this.dataSource.data = mapped;
        this.error = null;
        this.loading = false;
        try { this.cdr.detectChanges(); } catch (e) { }
      },
      error: (err: any) => {
        console.error('[UserManagement] loadUsers error', err);
        this.error = err?.error?.error || err?.error?.message || err?.message || 'Lỗi tải người dùng';
        this.loading = false;
        try { this.cdr.detectChanges(); } catch (e) { }
      }
    });
  }

  applyFilter(event: Event) {
    const filterValue = (event.target as HTMLInputElement).value;
    this.dataSource.filter = filterValue.trim().toLowerCase();
    if (this.dataSource.paginator) this.dataSource.paginator.firstPage();
  }

  openAddDialog() {
    // TODO: implement real add dialog; placeholder to log for now
    console.log('Open add user dialog');
  }

  openEditDialog(user: any) {
    // TODO: implement real edit dialog; placeholder to log for now
    console.log('Open edit dialog for user', user);
  }

  deleteUser(user: any) {
    if (!user || !user.id) return;
    if (!confirm('Are you sure you want to delete this user?')) return;
    this.userService.deleteUser(user.id).subscribe({
      next: () => {
        this.showMessage('User deleted');
        this.loadUsers();
      },
      error: (err: any) => {
        console.error('Error deleting user', err);
        this.showMessage('Error deleting user', true);
      }
    });
  }

  private showMessage(message: string, isError: boolean = false) {
    try {
      this.snackBar.open(message, 'Close', { duration: 3000, panelClass: isError ? ['error-snackbar'] : ['success-snackbar'] });
    } catch (e) { console.log(message); }
  }

  ngOnDestroy() { this.navSub?.unsubscribe(); }
}
