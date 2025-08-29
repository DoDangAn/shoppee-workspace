import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { Home } from './pages/home/home';
import { Products } from './pages/products/products';
import { Cart } from './pages/cart/cart';
import { ProductDetail } from './pages/product-detail/product-detail';
import { Login } from './pages/login/login';
import { Register } from './pages/register/register';
import { UserDashboard } from './pages/user-dashboard/user-dashboard';
import { AdminDashboard } from './pages/admin-dashboard/admin-dashboard';

const routes: Routes = [
  { path: '', component: Home },
  { path: 'home', component: Home },
  { path: 'products', component: Products },
  { path: 'cart', component: Cart },
  { path: 'product/:id', component: ProductDetail },
  { path: 'login', component: Login },
  { path: 'register', component: Register },
  { path: 'user-dashboard', component: UserDashboard },
  { path: 'admin-dashboard', component: AdminDashboard },
  { path: '**', redirectTo: '' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
