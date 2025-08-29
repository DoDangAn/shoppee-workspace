
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Api } from './api';

@Injectable({
  providedIn: 'root'
})
export class ProductService {
  constructor(
    private http: HttpClient,
    private api: Api
  ) {}

  getAll(): Observable<any[]> {
    return this.http.get<any[]>(this.api.getProductsUrl());
  }

  getById(id: string): Observable<any> {
    return this.http.get<any>(this.api.getProductUrl(id));
  }

  getVariants(productId: string): Observable<any[]> {
    return this.http.get<any[]>(this.api.getProductVariantsUrl(productId));
  }

  create(product: FormData): Observable<any> {
    return this.http.post<any>(this.api.getProductsUrl(), product);
  }

  update(id: string, product: FormData): Observable<any> {
    return this.http.put<any>(`${this.api.getProductUrl(id)}`, product);
  }

  delete(id: string): Observable<any> {
    return this.http.delete<any>(`${this.api.getProductUrl(id)}`);
  }
}
