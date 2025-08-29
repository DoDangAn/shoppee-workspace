import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, map, tap } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class CartService {
  private apiUrl = '/api/cart';
  private itemsSubject = new BehaviorSubject<any[]>([]);
  items$ = this.itemsSubject.asObservable();

  constructor(private http: HttpClient) {
    // Initial load
    this.http.get<any[]>(this.apiUrl).subscribe(list => this.itemsSubject.next(list || []));
  }

  getCart(): Observable<any[]> {
    return this.http.get<any[]>(this.apiUrl).pipe(tap(list => this.itemsSubject.next(list || [])));
  }

  addToCart(item: any): Observable<any> {
    return this.http.post<any>(this.apiUrl, item).pipe(tap(res => {
      // Backend returns {success, cart:[...]}
      if (res?.cart) this.itemsSubject.next(res.cart);
      else {
        // Fallback: refetch
        this.getCart().subscribe();
      }
    }));
  }

  updateCart(items: any[]): Observable<any> {
    return this.http.put<any>(this.apiUrl, items).pipe(tap(res => {
      if (res?.cart) this.itemsSubject.next(res.cart);
    }));
  }

  removeFromCart(index: number): Observable<any> {
    return this.http.delete<any>(`${this.apiUrl}/${index}`).pipe(tap(res => {
      if (res?.cart) this.itemsSubject.next(res.cart);
    }));
  }
}
