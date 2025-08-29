import { Route } from '@angular/router';
import { DashboardPage } from './pages/dashboard/dashboard';
import { AdminHomePage } from './pages/home/admin-home';
import { ProductManagement } from './pages/product-management/product-management';
import { CategoryManagement } from './pages/category-management/category-management';
import { UserManagementPage } from './pages/user-management/user-management';
import { OrderManagement } from './pages/order-management/order-management';
import { AdminLayout } from './pages/layout/admin-layout';
import { adminGuard } from '../../guards/admin-guard';
import { adminResolver } from '../../guards/admin-resolver';

export const routes: Route[] = [
  {
    path: '',
    component: AdminLayout,
  canActivate: [adminGuard],
  canActivateChild: [adminGuard],
  resolve: { ready: adminResolver },
    children: [
  { path: '', redirectTo: 'home', pathMatch: 'full' },
  { path: 'home', component: AdminHomePage },
  { path: 'dashboard', component: DashboardPage },
      { path: 'products', component: ProductManagement },
      { path: 'categories', component: CategoryManagement },
      { path: 'users', component: UserManagementPage },
      { path: 'orders', component: OrderManagement }
    ]
  }
];
