import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Group } from '../models/group';
import { Category } from '../models/category';
import { DateRange } from '../models/date.range';
import { Transaction } from '../models/transaction';
import { Credentials, Registration } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class ApiService {

  constructor(private http: HttpClient) { }

  login(credentials: Credentials): Observable<any> {
    return this.http.post<any>('/api/login', credentials);
  }

  register(registration: Registration): Observable<any> {
    return this.http.post<any>('/api/register', registration);
  }

  getGroups(opdate: string): Observable<Group[]> {
    return this.http.get<Group[]>(`/api/groups?opdate=${encodeURIComponent(opdate)}`);
  }

  getCategories(): Observable<Category[]> {
    return this.http.get<Category[]>('/api/categories');
  }

  getTransactions(accounts: number[], search: string, range: DateRange, category: number | null | undefined, currency: string, offset: number, limit: number): Observable<Transaction[]> {
    let params = new HttpParams();
    params = params.set('accounts', accounts.join(","));
    params = params.set('search', search);
    params = params.set('category', typeof category === 'number' ? category : '');
    params = params.set('currency', currency);
    params = params.set('from', range?.from?.toString('YMD','-') || '');
    params = params.set('to', range?.to?.toString('YMD','-') || '');
    params = params.set('offset', offset);
    params = params.set('limit', limit);
    return this.http.get<Transaction[]>('/api/transactions', {params: params});
  }

  saveTransaction(transaction: Transaction): Observable<Transaction> {
    return !!transaction.id ? this.http.put<Transaction>('/api/transactions', transaction) : this.http.post<Transaction>('/api/transactions', transaction);
  }

  deleteTransaction(id: number): Observable<void> {
    return this.http.delete<void>(`/api/transactions/${id}`);
  }
}
