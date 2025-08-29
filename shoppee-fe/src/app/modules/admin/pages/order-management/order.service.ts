import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Api } from '../../../../services/api';
import { catchError, switchMap } from 'rxjs/operators';
import { throwError } from 'rxjs';
import { AuthService } from '../../../../services/auth';

export interface OrderItem {
  id: number;
  productId: number;
  productName: string;
  quantity: number;
  price: number;
  total: number;
}

export interface Order {
  id: number;
  orderNumber: string;
  userId: number;
  userName: string;
  status: 'PENDING' | 'PROCESSING' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED';
  totalAmount: number;
  items: OrderItem[];
  shippingAddress: string;
  paymentMethod: string;
  createdAt: Date;
  updatedAt: Date;
}

@Injectable({
  providedIn: 'root'
})
export class OrderService {
  private apiUrl: string;

  constructor(private http: HttpClient, private api: Api, private auth: AuthService) {
    this.apiUrl = this.api.getAdminApiUrl('/orders');
  }

  getOrders(): Observable<Order[]> {
    const token = this.auth.getToken();
    const opts = token ? { headers: { Authorization: `Bearer ${token}` } } : undefined;
    return this.http.get<Order[]>(this.apiUrl, opts).pipe(
      catchError(err => {
        if (err?.status === 401 && this.auth.getToken()) {
          return this.auth.refreshProfile().pipe(switchMap(() => this.http.get<Order[]>(this.apiUrl, opts)));
        }
        return throwError(() => err);
      })
    );
  }

  getOrderById(id: number): Observable<Order> {
    return this.http.get<Order>(`${this.apiUrl}/${id}`);
  }

  updateOrderStatus(id: number, status: string): Observable<Order> {
    const token = this.auth.getToken();
    const opts = token ? { headers: { Authorization: `Bearer ${token}` } } : undefined;
  const url = `${this.apiUrl}/${id}/status`;
  console.log('[OrderService] updateOrderStatus request', { url, body: { status }, hasToken: !!token });
    return this.http.patch<Order>(url, { status }, opts).pipe(
      catchError(err => {
        console.error('[OrderService] updateOrderStatus error', err);
        if (err?.status === 401 && this.auth.getToken()) {
          return this.auth.refreshProfile().pipe(switchMap(() => this.http.patch<Order>(url, { status }, opts)));
        }
        return throwError(() => err);
      })
    );
  }
}
