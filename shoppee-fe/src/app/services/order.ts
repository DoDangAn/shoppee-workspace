
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Api } from './api';

@Injectable({
  providedIn: 'root'
})
export class OrderService {
  constructor(
    private http: HttpClient,
    private api: Api
  ) {}

  getAll(): Observable<any[]> {
    return this.http.get<any[]>(this.api.getOrderUrl());
  }

  getById(orderId: string): Observable<any> {
    return this.http.get<any>(`${this.api.getOrderUrl()}/${orderId}`);
  }

  create(order: any): Observable<any> {
    return this.http.post<any>(this.api.getOrderUrl(), order);
  }

  update(orderId: string, order: any): Observable<any> {
    return this.http.put<any>(`${this.api.getOrderUrl()}/${orderId}`, order);
  }

  delete(orderId: string): Observable<any> {
    return this.http.delete<any>(`${this.api.getOrderUrl()}/${orderId}`);
  }

  createVNPayPayment(orderInfoId: string, bankCode: string, locale: string, ipAddr: string): Observable<any> {
    const params = { bankCode, locale, ipAddr };
    return this.http.post<any>(this.api.getVNPayUrl(orderInfoId), null, { params });
  }

  cancelOrder(orderId: string): Observable<any> {
    return this.http.patch<any>(`${this.api.getOrderUrl()}/${orderId}/cancel`, null);
  }
}
