import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { HomeResponse } from '../models/home.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class HomeService {
  private apiUrl = `${environment.apiUrl}/user/home`;

  constructor(private http: HttpClient) {}

  getHomeData(field?: string, field2?: string, field3?: string): Observable<HomeResponse> {
    let params = new HttpParams();
    if (field) params = params.set('field', field);
    if (field2) params = params.set('field2', field2);
    if (field3) params = params.set('field3', field3);

    return this.http.get<HomeResponse>(this.apiUrl, { params });
  }
}