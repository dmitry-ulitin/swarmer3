import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Group } from '../models/group';
import { Category } from '../models/category';
import { DateRange } from '../models/date.range';
import { Transaction, TransactionImport, TransactionType } from '../models/transaction';
import { Credentials, Registration } from './auth.service';
import { Rule } from '../models/rule';
import { Summary } from '../models/summary';
import { CategorySum } from '../models/category.sum';

@Injectable({
  providedIn: 'root'
})
export class ApiService {

  constructor(private http: HttpClient) { }

  login(credentials: Credentials): Observable<any> {
    return this.http.post<any>('/api/auth/login', credentials);
  }

  register(registration: Registration): Observable<any> {
    return this.http.post<any>('/api/auth/register', registration);
  }

  getGroups(opdate: string): Observable<Group[]> {
    return this.http.get<Group[]>(`/api/groups?opdate=${encodeURIComponent(opdate)}`);
  }

  saveGroup(group: Group): Observable<Group> {
    return !!group.id ? this.http.put<Group>('/api/groups', group) : this.http.post<Group>('/api/groups', group);
  }

  deleteGroup(id: number): Observable<void> {
    return this.http.delete<void>(`/api/groups/${id}`);
  }

  getCategories(): Observable<Category[]> {
    return this.http.get<Category[]>('/api/categories');
  }

  saveCategory(category: Category) {
    return !!category.id ? this.http.put<Category>('/api/categories', category) : this.http.post<Category>('/api/categories', category);
  }

  getRules(): Observable<Rule[]> {
    return this.http.get<Rule[]>('/api/transactions/rules');
  }

  deleteRule(id: number): Observable<void> {
    return this.http.delete<void>(`/api/transactions/${id}`);
  }

  addRule(rule: Rule): Observable<Rule> {
    return this.http.post<Rule>('/api/transactions/rules', rule);
  }

  updateRule(rule: Rule): Observable<Rule> {
    return this.http.put<Rule>(`/api/transactions/rules/${rule.id}`, rule);
  }

  saveRule(rule: Rule): Observable<Rule> {
    return !!rule.id ? this.updateRule(rule) : this.addRule(rule);
  }

  deleteCategory(id: number): Observable<void> {
    return this.http.delete<void>(`/api/categories/${id}`);
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

  getUsers(query: string): Observable<string[]> {
    let params = new HttpParams();
    params = params.set('query', query);
    return this.http.get<string[]>('/api/groups/users', {params: params});
  }

  getBackup(): Observable<HttpResponse<Blob>> {
    return this.http.get('/api/data/dump', {responseType: 'blob', observe: 'response'});
  }

  loadBackup(blob: any) {
    return this.http.put('/api/data/dump', blob);
  }

  importTransactions(acc: number, bank: number, file: File): Observable<TransactionImport[]> {
    const formData: FormData = new FormData();
    formData.append('file', file, file.name);
    formData.append('id', acc.toString());
    formData.append('bank', bank.toString());
    return this.http.post<TransactionImport[]>('/api/transactions/import', formData);
  }

  saveTransactions(acc: number, transactions: TransactionImport[]): Observable<void> {
    return this.http.patch<void>(`/api/transactions/import?account=${acc}`, transactions);
  }

  getSummary(accounts: number[], range: DateRange): Observable<Summary[]> {
    let params = new HttpParams();
    params = params.set('accounts', accounts.join(","));
    params = params.set('from', range?.from?.toString('YMD','-') || '');
    params = params.set('to', range?.to?.toString('YMD','-') || '');
    return this.http.get<Summary[]>('/api/transactions/summary', {params: params});
  }

  getCategoriesSummary(type: TransactionType, accounts: number[], range: DateRange): Observable<CategorySum[]> {
    let params = new HttpParams();
    params = params.set('type', type.toString());
    params = params.set('accounts', accounts.join(","));
    params = params.set('from', range?.from?.toString('YMD','-') || '');
    params = params.set('to', range?.to?.toString('YMD','-') || '');
    return this.http.get<CategorySum[]>('/api/transactions/categories', {params: params});
  }
}
