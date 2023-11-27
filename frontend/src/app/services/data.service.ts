import { Injectable, computed, inject, signal } from '@angular/core';
import { ApiService } from './api.service';
import { Group, total } from '../models/group';
import { firstValueFrom } from 'rxjs';
import { Category } from '../models/category';
import { AlertService } from './alert.service';
import { Account } from '../models/account';

export interface DataState {
  // groups
  groups: Group[];
  expanded: number[];
  accounts: number[];
  // categories
  categories: Category[];
  // filters
  currency: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class DataService {
  #api = inject(ApiService);
  #default: DataState = { groups: [], expanded: [], accounts: [], categories: [], currency: null }
  #state = signal<DataState>(this.#default);
  #alerts = inject(AlertService);
  // selectors
  state = this.#state.asReadonly();
  groups = computed(() => this.#state().groups.filter(g => !g.deleted));
  allAccounts = computed(() => this.#state().groups.filter(g => !g.deleted).reduce((acc, g) => acc.concat(g.accounts), [] as Account[]).filter(a => !a.deleted));
  selectedAccounts = computed(() => this.allAccounts().filter(a => this.#state().accounts.includes(a.id)));
  total = computed(() => total(this.#state().groups));

  async init() {
    await Promise.all([this.getGroups(), this.getCategories()]);
  }

  reset() {
    this.#state.set({ ...this.#default });
  }

  async getGroups() {
    try {
      const groups = await firstValueFrom(this.#api.getGroups(''));
      this.#state.update(state => ({ ...state, groups }));
    } catch (err) {
      this.#alerts.printError(err);
    }
  }

  toggleGropup(id: number) {
    if ((this.#state().groups.find(g => g.id === id)?.accounts.length || 0) > 1) {
      let expanded = this.#state().expanded.filter(id => id !== id);
      if (expanded.length === this.#state().expanded.length) {
        expanded.push(id);
      }
      this.#state.update(state => ({ ...state, expanded }));
    }
  }

  selectAccounts(accounts: number[]) {
    this.#state.update(state => ({ ...state, accounts }));
    const state = this.#state();
    if (!!state.currency) {
      const currencies = this.selectedAccounts().map(a => a.currency);
      if (!currencies.includes(state.currency)) {
        this.#state.update(state => ({ ...state, currency: null }));
      }
    }
    //    cxt.dispatch(new GetTransactions());
    //    cxt.dispatch(new GetSummary());
  }

  async getCategories() {
    try {
      const categories = await firstValueFrom(this.#api.getCategories());
      this.#state.update(state => ({ ...state, categories }));
    } catch (err) {
      this.#alerts.printError(err);
    }
  }
}
