import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { Product } from '../models/product.model';

export interface CartItem {
  id: number;
  product: Product;
  quantity: number;
}

export interface Cart {
  id?: number;
  userId?: number;
  items: CartItem[];
  total: number;
}

@Injectable({
  providedIn: 'root'
})
export class CartService {
  private apiUrl = 'http://localhost:8080/api/cart';
  private cartSubject = new BehaviorSubject<Cart>({ items: [], total: 0 });
  public cart$ = this.cartSubject.asObservable();

  constructor(private http: HttpClient) { }

  getCart(): Observable<Cart> {
    return this.http.get<Cart>(this.apiUrl);
  }

  addToCart(productId: number, quantity: number = 1): Observable<Cart> {
    return this.http.post<Cart>(`${this.apiUrl}/add`, { productId, quantity });
  }

  updateCartItem(itemId: number, quantity: number): Observable<Cart> {
    return this.http.put<Cart>(`${this.apiUrl}/items/${itemId}`, { quantity });
  }

  removeFromCart(itemId: number): Observable<Cart> {
    return this.http.delete<Cart>(`${this.apiUrl}/items/${itemId}`);
  }

  clearCart(): Observable<any> {
    return this.http.delete(this.apiUrl);
  }

  checkout(): Observable<any> {
    return this.http.post(`${this.apiUrl}/checkout`, {});
  }

  // Local cart management
  updateLocalCart(cart: Cart): void {
    this.cartSubject.next(cart);
  }

  getLocalCart(): Cart {
    return this.cartSubject.value;
  }
} 