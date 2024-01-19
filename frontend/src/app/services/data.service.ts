import { Injectable, computed, inject, signal } from '@angular/core';
import { ApiService } from './api.service';
import { Group, total } from '../models/group';
import { firstValueFrom } from 'rxjs';
import { Category } from '../models/category';
import { AlertService } from './alert.service';
import { Account } from '../models/account';
import { Summary } from '../models/summary';
import { Transaction } from '../models/transaction';
import { DateRange } from '../models/date.range';

const GET_TRANSACTIONS_LIMIT = 100;

export interface DataState {
  // groups
  groups: Group[];
  expanded: number[];
  // transactions
  transactions: Transaction[];
  tid: number | null | undefined;
  summary: Summary[];
  // categories
  categories: Category[];
  // filters
  search: string;
  accounts: number[];
  range: DateRange;
  category: Category | null;
  currency: string;
}

@Injectable({
  providedIn: 'root'
})
export class DataService {
  #api = inject(ApiService);
  #default: DataState = {
    groups: [], expanded: [], transactions: [], tid: null, summary: [], categories: [],
    search: '', accounts: [], range: DateRange.last30(), category: null, currency: ''
  }
  #state = signal<DataState>(this.#default);
  #alerts = inject(AlertService);
  // selectors
  state = this.#state.asReadonly();
  groups = computed(() => this.#state().groups.filter(g => !g.deleted));
  allAccounts = computed(() => this.#state().groups.filter(g => !g.deleted).reduce((acc, g) => acc.concat(g.accounts), [] as Account[]).filter(a => !a.deleted));
  selectedAccounts = computed(() => this.allAccounts().filter(a => this.#state().accounts.includes(a.id)));
  total = computed(() => total(this.#state().groups));

  async init() {
    await Promise.all([this.getGroups(), this.getCategories(), this.getTransactions(this.#default)]);
  }

  reset() {
    this.#state.set({ ...this.#default });
  }

  async getGroups() {
    try {
      const groups = await firstValueFrom(this.#api.getGroups(''), { defaultValue: [] });
      this.#state.update(state => ({ ...state, groups }));
    } catch (err) {
      this.#alerts.printError(err);
    }
  }

  toggleGropup(id: number) {
    if ((this.#state().groups.find(g => g.id === id)?.accounts.length || 0) > 1) {
      let expanded = this.#state().expanded.filter(e => e !== id);
      if (expanded.length === this.#state().expanded.length) {
        expanded.push(id);
      }
      this.#state.update(state => ({ ...state, expanded }));
    }
  }

  createGroup() { }

  selectAccounts(accounts: number[]) {
    this.#state.update(state => ({ ...state, accounts }));
    const state = this.#state();
    if (!!state.currency) {
      const currencies = this.selectedAccounts().map(a => a.currency);
      if (!currencies.includes(state.currency)) {
        this.#state.update(state => ({ ...state, currency: '' }));
      }
    }
    this.getTransactions(state).then();
    //    cxt.dispatch(new GetTransactions());
    //    cxt.dispatch(new GetSummary());
  }

  async getCategories() {
    try {
      const categories = await firstValueFrom(this.#api.getCategories(), { defaultValue: [] });
      this.#state.update(state => ({ ...state, categories }));
    } catch (err) {
      this.#alerts.printError(err);
    }
  }

  async getTransactions(state: DataState) {
    try {
      const transactions = await firstValueFrom(this.#api.getTransactions(state.accounts, state.search, state.range, state.category?.id, state.currency, 0, GET_TRANSACTIONS_LIMIT), { defaultValue: [] });
      const tid = transactions.find(t => t.id === state.tid)?.id;
      this.#state.update(state => ({ ...state, transactions, tid }));
    } catch (err) {
      this.#alerts.printError(err);
    }

  }
}
