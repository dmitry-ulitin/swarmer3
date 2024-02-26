import { Injectable, computed, inject, signal } from '@angular/core';
import { ApiService } from './api.service';
import { Group, total } from '../models/group';
import { firstValueFrom } from 'rxjs';
import { Category } from '../models/category';
import { AlertService } from './alert.service';
import { Account } from '../models/account';
import { Summary } from '../models/summary';
import { Transaction, TransactionView } from '../models/transaction';
import { DateRange } from '../models/date.range';

const GET_TRANSACTIONS_LIMIT = 50;

export interface DataState {
  // groups
  groups: Group[];
  expanded: number[];
  // transactions
  transactions: TransactionView[];
  tid: number | null | undefined;
  summary: Summary[];
  loaded: boolean;
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
    groups: [], expanded: [], transactions: [], tid: null, summary: [], loaded: false, categories: [],
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
      const selected: { [key: number]: boolean } = Object.assign({}, ...state.accounts.map(a => ({ [a]: true })));
      const tid = transactions.find(t => t.id === state.tid)?.id;
      this.#state.update(state => ({ ...state, tid, loaded: false, transactions: transactions.map(t => this.transaction2View(t, selected)) }));
    } catch (err) {
      this.#alerts.printError(err);
    }

  }

  selectTransaction(tid: number) {
    this.#state.update(state => ({ ...state, tid }));
  }


  async scrollTransactions() {
    try {
      const state = this.#state();
      if (!state.loaded) {
        const transactions = await firstValueFrom(this.#api.getTransactions(state.accounts, state.search, state.range, state.category?.id, state.currency, state.transactions.length, GET_TRANSACTIONS_LIMIT), { defaultValue: [] });
        const loaded = transactions.length < GET_TRANSACTIONS_LIMIT;
        const selected: { [key: number]: boolean } = Object.assign({}, ...state.accounts.map(a => ({ [a]: true })));
        this.#state.update(state => ({ ...state, loaded, transactions: state.transactions.concat(transactions.map(t => this.transaction2View(t, selected))) }));
      }
    } catch (err) {
      this.#alerts.printError(err);
    }

  }

  transaction2View(t: Transaction, selected: { [key: number]: boolean }): TransactionView {
    const useRecipient = t.recipient && (typeof t.account?.balance !== 'number' || typeof t.recipient?.balance === 'number' && selected[t.recipient?.id] && (!t.account || !selected[t.account?.id]));
    const amount = (t.account && !useRecipient) ? { value: t.debit, currency: t.account.currency } : { value: t.credit, currency: t.recipient.currency };
    const acc = useRecipient && t.recipient ? t.recipient : (t.account || t.recipient);
    return { ...t, amount, balance: { fullname: acc.fullname, currency: acc.currency, balance: acc.balance } };
  }

}
