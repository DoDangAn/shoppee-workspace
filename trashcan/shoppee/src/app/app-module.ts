
import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientModule, provideHttpClient, withInterceptors } from '@angular/common/http';
import { AppRoutingModule } from './app-routing-module';
import { authInterceptor } from './interceptors/auth-interceptor';
import { FormsModule } from '@angular/forms';
import { ReactiveFormsModule } from '@angular/forms';

// Material imports
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { App } from './app';
import { Products } from './pages/products/products';
import { Cart } from './pages/cart/cart';
import { ProductDetail } from './pages/product-detail/product-detail';
import { Header } from './layout/header/header';
import { Footer } from './layout/footer/footer';
import { Login } from './pages/login/login';
import { Register } from './pages/register/register';
import { UserDashboard } from './pages/user-dashboard/user-dashboard';
import { AdminDashboard } from './pages/admin-dashboard/admin-dashboard';

@NgModule({
  declarations: [
    App,
    Products,
    Cart,
    ProductDetail,
    Header,
    Footer,
    Login,
    Register,
    UserDashboard,
    AdminDashboard
  ],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    HttpClientModule,
    AppRoutingModule,
    FormsModule,
    ReactiveFormsModule,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatProgressSpinnerModule
  ],
  providers: [
    provideHttpClient(withInterceptors([authInterceptor]))
  ],
  bootstrap: [App]
})
export class AppModule {}
