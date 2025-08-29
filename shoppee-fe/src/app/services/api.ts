import { Injectable } from '@angular/core';
import { environment } from 'environments/environment';

@Injectable({
  providedIn: 'root'
})
export class Api {
  private apiUrl = environment.apiUrl.replace(/\/$/, ''); // remove trailing slash if any
  private adminApiUrl = `${this.apiUrl}/admin`;
  
  // Collapse multiple slashes (except after protocol) to prevent '//' in paths which can be
  // rejected by some servers/firewalls. Preserves 'http://' and 'https://'.
  private normalize(url: string): string {
    return url.replace(/([^:]\/)\/+/g, '$1');
  }

  // User APIs
  getUserApiUrl(endpoint: string): string {
    // ensure endpoint starts with a single '/'
  const ep = endpoint.startsWith('/') ? endpoint : `/${endpoint}`;
  return this.normalize(`${this.apiUrl}${ep}`);
  }

  // Admin APIs
  getAdminApiUrl(endpoint: string): string {
  const ep = endpoint.startsWith('/') ? endpoint : `/${endpoint}`;
  return this.normalize(`${this.adminApiUrl}${ep}`);
  }

  // Products
  getProductsUrl(isAdmin = false): string {
    return isAdmin ? this.getAdminApiUrl('/products') : this.getUserApiUrl('/products');
  }

  getProductUrl(id: string, isAdmin = false): string {
    return isAdmin ? this.getAdminApiUrl(`/product/${id}`) : this.getUserApiUrl(`/product/${id}`);
  }

  getProductVariantsUrl(productId: string): string {
    return this.getAdminApiUrl(`/product/variants/${productId}`);
  }

  // Categories
  getCategoriesUrl(): string {
    return this.getAdminApiUrl('/categories');
  }

  // Home
  getHomeUrl(isAdmin = false): string {
    return isAdmin ? this.getAdminApiUrl('/home') : this.getUserApiUrl('/home');
  }

  // Orders
  getOrderUrl(isAdmin = false): string {
    // Current backend user orders: /api/order ; admin list (future): /api/admin/order or /api/admin/orders
    return isAdmin ? this.getAdminApiUrl('/order') : this.getUserApiUrl('/order');
  }

  // Users
  getUsersUrl(): string {
    return this.getAdminApiUrl('/users');
  }

  getUserDetailsUrl(id: string): string {
    return this.getAdminApiUrl(`/user/${id}`);
  }

  // Payment
  getVNPayUrl(orderInfoId: string): string {
    return this.getUserApiUrl(`/payment/vnpay/${orderInfoId}`);
  }
}
