import { Injectable, computed, inject, signal } from '@angular/core';
import { ApiService } from './api.service';
import { Group, total } from '../models/group';
import { firstValueFrom } from 'rxjs';
import { Category } from '../models/category';
import { AlertService } from './alert.service';
import { Account } from '../models/account';
import { Summary } from '../models/summary';
import { Transaction, TransactionType, TransactionView } from '../models/transaction';
import { DateRange } from '../models/date.range';
import { TuiDialogService } from '@taiga-ui/core';
import { PolymorpheusComponent } from '@tinkoff/ng-polymorpheus';
import { TrxEditorComponent } from '../trx.editor/trx.editor.component';

const GET_TRANSACTIONS_LIMIT = 100;

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
  #dlgService = inject(TuiDialogService);
  // selectors
  state = this.#state.asReadonly();
  groups = computed(() => this.#state().groups.filter(g => !g.deleted));
  allAccounts = computed(() => this.#state().groups.filter(g => !g.deleted).reduce((acc, g) => acc.concat(g.accounts), [] as Account[]).filter(a => !a.deleted));
  selectedAccounts = computed(() => this.allAccounts().filter(a => this.#state().accounts.includes(a.id)));
  total = computed(() => total(this.#state().groups));
  currencies = computed(() => Array.from(new Set(this.allAccounts().map(a => a.currency))).filter((v, i, a) => a.indexOf(v) === i));

  async init() {
    await this.refresh();
  }

  reset() {
    this.#state.set({ ...this.#default });
  }

  async refresh() {
    await Promise.all([this.getGroups(), this.getCategories(), this.getTransactions(this.#state())]);
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
    const prev = this.#state().accounts;
    if (accounts.every(a => prev.includes(a)) && prev.every(a => accounts.includes(a))) {
      return;
    }
    this.#state.update(state => ({ ...state, accounts }));
    const state = this.#state();
    // check expanded
    let expanded = this.#state().expanded;
    for (let aid of state.accounts) {
      const group = state.groups.find(g => g.accounts.findIndex(a => a.id == aid) >= 0);
      if (group?.id && group.accounts.length > 1 && !expanded.includes(group.id) && group.accounts.some(a => !a.deleted && !state.accounts.includes(a.id))) {
        expanded.push(group.id);
        this.#state.update(state => ({ ...state, expanded }));
      }
    }
    // check currency
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

  selectCategory(category: Category | null) {
    this.#state.update(state => ({ ...state, category }));
    this.getTransactions(this.#state()).then();
  }

  async getTransactions(state: DataState) {
    try {
      const transactions = await firstValueFrom(this.#api.getTransactions(state.accounts, state.search, state.range, state.category?.id, state.currency, 0, GET_TRANSACTIONS_LIMIT), { defaultValue: [] });
      const loaded = transactions.length < GET_TRANSACTIONS_LIMIT;
      const selected: { [key: number]: boolean } = Object.assign({}, ...state.accounts.map(a => ({ [a]: true })));
      const tid = transactions.find(t => t.id === state.tid)?.id;
      this.#state.update(state => ({ ...state, tid, loaded, transactions: transactions.map(t => transaction2View(t, selected)) }));
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
        this.#state.update(state => ({ ...state, loaded, transactions: state.transactions.concat(transactions.map(t => transaction2View(t, selected))) }));
      }
    } catch (err) {
      this.#alerts.printError(err);
    }

  }

  async createTransaction(type: TransactionType) {
    const state = this.#state();
    const accounts = this.allAccounts();
    if (accounts.length === 0) {
      this.#alerts.printError('No accounts found');
      return;
    }
    const first = state.transactions.find((t: TransactionView) => t.type !== TransactionType.Transfer);
    const account = first?.account || first?.recipient || accounts.find(a => a.id === state.accounts[0]) || accounts[0];

    const date = new Date();
    let base = { type, opdate: date.toISOString().substring(0,10) + ' ' + date.toTimeString().substring(0,8), debit: 0, credit: 0, id: 0 };
    let transaction = null;
    if (type === TransactionType.Transfer) {
      const transfer = this.state().transactions.find((t: TransactionView) => t.type === TransactionType.Transfer);
      const recipient = transfer?.recipient ||
        accounts.find(a => a.id !== account?.id && a.currency === account?.currency) ||
        accounts.find(a => a.id !== account?.id);
      transaction = { ...base, account: transfer?.account || account, recipient };
    } else {
      const category = type === TransactionType.Correction ? state.categories.find(c => c.type === TransactionType.Correction) : null;
      transaction = type === TransactionType.Income ? { ...base, category, recipient: account } : { ...base, category, account };
    }
    const data = await firstValueFrom(this.#dlgService.open<Transaction | undefined>(
      new PolymorpheusComponent(TrxEditorComponent), { data: transaction, dismissible: false, size: 's' }
    ));
    if (!!data) {
      this.#alerts.printSuccess('Transaction created');
      this.patchStateTransactions(data, false);
      if (state.categories.findIndex(c => c.id === data.category?.id) < 0) {
        this.getCategories();
      }
    }
  }

  patchStateTransactions(transaction: Transaction, remove: boolean) {
    const state = this.#state();
    const transactions = state.transactions.slice();
    const index = remove ? transactions.findIndex(t => t.id === transaction.id) : Math.max(transactions.findIndex(t => transaction.opdate == t.opdate && (transaction.id || 0) > (t.id || 0) || transaction.opdate > t.opdate), 0);
    if (index >= 0) {
      // patch transactions balances
      const selected: { [key: number]: boolean } = Object.assign({}, ...state.accounts.map(a => ({ [a]: true })));
      for (let i = index - 1; i >= 0; i--) {
        let apatch = 0;
        let rpatch = 0;
        let trx = { ...transactions[i] };
        if (trx.account && trx.account?.id === transaction.account?.id) {
          apatch = -transaction.debit;
        }
        if (trx.recipient && trx.recipient?.id === transaction.account?.id) {
          rpatch = -transaction.debit;
        }
        if (trx.account && trx.account?.id === transaction.recipient?.id) {
          apatch = transaction.credit;
        }
        if (trx.recipient && trx.recipient?.id === transaction.recipient?.id) {
          rpatch = transaction.credit;
        }
        if (remove) {
          apatch = -apatch;
          rpatch = -rpatch;
        }
        if (!!trx.account && typeof trx.account.balance === 'number' && (trx.account?.id === transaction.account?.id || trx.account?.id === transaction.recipient?.id)) {
          trx.account.balance += apatch;
        }
        if (!!trx.recipient && typeof trx.recipient.balance === 'number' && (trx.recipient?.id === transaction.account?.id || trx.recipient?.id === transaction.recipient?.id)) {
          trx.recipient.balance += rpatch;
        }
        if (trx.category?.id == TransactionType.Correction) {
          trx.credit += rpatch;
          if (trx.credit < 0) {
            trx.credit = -trx.credit;
            if (!!trx.recipient) {
              trx = { ...trx, account: trx.recipient, recipient: null };
            } else {
              trx = { ...trx, account: null, recipient: trx.account };
            }
          }
          trx.debit = trx.credit;
        }
        transactions[i] = transaction2View(trx, selected);
      }
      if (remove) {
        transactions.splice(index, 1);
      } else {
        transactions.splice(index, 0, transaction2View(transaction, selected));
      }
    }
    // patch group balances
    const groups = state.groups.slice();
    patchGroupBalance(groups, transaction.account, remove ? transaction.debit : -transaction.debit);
    patchGroupBalance(groups, transaction.recipient, remove ? -transaction.credit : transaction.credit);
    const tid = remove && transaction.id === state.tid ? null : (remove ? state.tid : transaction.id);
    // patch summary
    const summary = state.summary.slice();
    for (let s of summary) {
      if (!!transaction.account?.id) {
        if (!!transaction.recipient?.id) {
          if (transaction.account.currency === s.currency && state.accounts.includes(transaction.account.id) && !state.accounts.includes(transaction.recipient.id)) {
            s.transfers_debit += remove ? -transaction.debit : transaction.debit;
          } else if (transaction.recipient.currency === s.currency && !state.accounts.includes(transaction.account.id) && state.accounts.includes(transaction.recipient.id)) {
            s.transfers_credit += remove ? -transaction.credit : transaction.credit;
          }
        } else if (transaction.account.currency === s.currency) {
          s.debit += remove ? -transaction.debit : transaction.debit;
        }
      } else if (!!transaction.recipient?.id && transaction.recipient.currency === s.currency) {
        s.credit += remove ? -transaction.credit : transaction.credit;
      }
    }
    // patch state
    this.#state.update(state => ({ ...state, transactions, groups, tid, summary }));
  }
}


function transaction2View(t: Transaction, selected: { [key: number]: boolean }): TransactionView {
  const useRecipient = t.recipient && (typeof t.account?.balance !== 'number' || typeof t.recipient?.balance === 'number' && selected[t.recipient?.id] && (!t.account || !selected[t.account?.id]));
  const amount = (t.account && !useRecipient) ? { value: t.debit, currency: t.account.currency } : { value: t.credit, currency: t.recipient.currency };
  const acc = useRecipient && t.recipient ? t.recipient : (t.account || t.recipient);
  return { ...t, amount, balance: { aid: acc.id, fullname: acc.fullname, currency: acc.currency, balance: acc.balance } };
}

function patchGroupBalance(groups: Group[], account: Account | null, amount: number) {
  if (account) {
    const gindex = groups.findIndex(g => g.accounts.find(a => a.id === account.id));
    if (gindex >= 0) {
      groups[gindex] = { ...groups[gindex], accounts: groups[gindex].accounts.slice() };
      const aindex = groups[gindex].accounts.findIndex(a => a.id === account.id);
      const acc = groups[gindex].accounts[aindex];
      if (typeof acc.balance === 'number') {
        groups[gindex].accounts = [...groups[gindex].accounts];
        groups[gindex].accounts[aindex] = { ...acc, balance: acc.balance + amount };
      }
    }
  }
}
