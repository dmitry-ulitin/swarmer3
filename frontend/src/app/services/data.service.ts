import { TUI_CONFIRM } from "@taiga-ui/kit";
import { Injectable, computed, inject, signal } from '@angular/core';
import { ApiService } from './api.service';
import { Group, total } from '../models/group';
import { firstValueFrom, forkJoin } from 'rxjs';
import { Category } from '../models/category';
import { AlertService } from './alert.service';
import { Account } from '../models/account';
import { Summary } from '../models/summary';
import { Transaction, TransactionImport, TransactionType, TransactionView } from '../models/transaction';
import { DateRange } from '../models/date.range';
import { TuiDialogService } from '@taiga-ui/core';
import { PolymorpheusComponent } from '@taiga-ui/polymorpheus';
import { TrxEditorComponent } from '../trx.editor/trx.editor.component';
import { GrpEditorComponent } from '../grp.editor/grp.editor.component';
import { CatEditorComponent } from '../cat.editor/cat.editor.component';
import { LoadDumpComponent } from '../load/load.dump.component';
import { LoadStatComponent } from '../load/load.stat.component';
import { StatementComponent } from '../statement/statement.component';
import { Rule } from '../models/rule';
import { RuleEditorComponent } from '../rule.editor/rule.editor.component';
import { CategorySum } from '../models/category.sum';
import { AccEditorComponent } from "../acc.editor/acc.editor.component";

const GET_TRANSACTIONS_LIMIT = 100;

export interface DataState {
  // groups
  groups: Group[];
  expanded: number[];
  // transactions
  transactions: TransactionView[];
  loaded: boolean;
  tid: number | null | undefined;
  summary: Summary[];
  income: CategorySum[];
  expenses: CategorySum[];
  // categories
  categories: Category[];
  // rules
  rules: Rule[];
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
    groups: [], expanded: [], transactions: [], loaded: false, tid: null, summary: [], income: [], expenses: [], categories: [],
    rules: [], search: '', accounts: [], range: DateRange.last30(), category: null, currency: ''
  }
  #state = signal<DataState>(this.#default);
  #alerts = inject(AlertService);
  #dlgService = inject(TuiDialogService);
  // selectors
  state = this.#state.asReadonly();
  groups = computed(() => this.#state().groups.filter(g => !g.deleted));
  allAccounts = computed(() => this.#state().groups.filter(g => !g.deleted).reduce((acc, g) => acc.concat(g.accounts), [] as Account[]).filter(a => !a.deleted));
  selectedAccounts = computed(() => this.allAccounts().filter(a => this.#state().accounts.includes(a.id)));
  selectedAccount = computed(() => this.selectedAccounts().length === 1 ? this.selectedAccounts()[0] : null);
  selectedGroups = computed(() => this.#state().groups.filter(g => g.accounts.some(a => this.#state().accounts.includes(a.id))));
  selectedGroup = computed(() => this.selectedGroups().length === 1 ? this.selectedGroups()[0] : null);
  total = computed(() => total(this.#state().groups));
  currencies = computed(() => Array.from(new Set(this.allAccounts().map(a => a.currency))).filter((v, i, a) => a.indexOf(v) === i));
  filters = computed(() => {
    const state = this.#state();
    return state.groups.map(g => ({ group: g, accounts: g.accounts.filter(a => state.accounts.includes(a.id)) }))
      .filter(f => f.accounts.length > 0)
      .map(f => ({ name: f.group.fullName, accounts: f.accounts, selected: !f.group.accounts.filter(a => !a.deleted).some(a => !state.accounts.includes(a.id)) }))
      .reduce((acc, f) => {
        if (f.selected) {
          acc.push({ name: f.name, ids: f.accounts.map(a => a.id) });
        } else {
          acc = acc.concat(f.accounts.map(a => ({ name: a.fullName, ids: [a.id] })));
        }
        return acc;
      }, [] as { name: string, ids: number[] }[]);
  });

  async init() {
    await this.refresh();

    let max = this.#state().groups.map(g => g.opdate || '').reduce((max, c) => c > max ? c : max, '');
    if (max < (this.#state().range.from?.toString('YMD', '-') || '')) {
      await this.setRange(DateRange.all());
    }
  }

  reset() {
    this.#state.set({ ...this.#default });
  }

  async refresh() {
    await Promise.all([this.getGroups(), this.getCategories(), this.getRules(), this.getTransactions(this.#state()), this.getSummary(), this.getCategoriesSummary(), this.checkWallets(this.#state().accounts, false)]);
  }

  async getGroups() {
    try {
      const groups = await firstValueFrom(this.#api.getGroups(''), { defaultValue: [] });
      this.#state.update(state => ({ ...state, groups }));
    } catch (err) {
      this.#alerts.printError(err);
    }
  }


  async getGroup(id: number) {
    try {
      const group = await firstValueFrom(this.#api.getGroup(id), { defaultValue: null });
      if (group) {
        this.#state.update(state => ({ ...state, groups: state.groups.map(g => g.id === id ? group : g) }));
      }
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

  async createGroup() {
    const group: Group = { id: 0, fullName: '', owner: true, coowner: false, shared: false, accounts: [{ id: 0, name: '', fullName: '', currency: '', chain: '', address: '', scale: 2, startBalance: 0, balance: 0 }], permissions: [] };
    const data = await firstValueFrom(this.#dlgService.open<Group | undefined>(
      new PolymorpheusComponent(GrpEditorComponent), { data: group, dismissible: false, closeable: false, size: 's' }
    ));
    if (!!data) {
      this.#alerts.printSuccess(`Group '${data.fullName}' created`);
      await Promise.all([this.getGroups(), this.checkWallets(data.accounts.map(a => a.id), true)]);
    }
  }

  async editGroup(id: number) {
    try {
      const group = await firstValueFrom(this.#api.getGroup(id), { defaultValue: null });
      if (!!group) {
        const data = await firstValueFrom(this.#dlgService.open<Group | undefined>(
          new PolymorpheusComponent(GrpEditorComponent), { data: group, dismissible: false, closeable: false, size: 's' }
        ));
        if (!!data) {
          this.#alerts.printSuccess(`Group '${data.fullName}' updated`);
          await Promise.all([this.getGroups(), this.checkWallets(data.accounts.map(a => a.id), true)]);
        }
      }
    } catch (err) {
      this.#alerts.printError(err);
    }
  }

  async deleteGroup(id: number) {
    try {
      const group = this.#state().groups.find(g => g.id === id);
      if (!!group) {
        let answer = await firstValueFrom(this.#dlgService.open<boolean>(TUI_CONFIRM, { size: 's', data: { content: 'Are you sure you want to delete this group?', yes: 'Yes', no: 'No' } }), { defaultValue: false });
        if (answer) {
          const force = group.accounts.some(a => a.balance !== 0);
          if (force) {
            answer = await firstValueFrom(this.#dlgService.open<boolean>(TUI_CONFIRM, { size: 's', data: { content: 'This group has accounts with non-zero balance. Are you sure you want to delete it?', yes: 'Yes', no: 'No' } }), { defaultValue: false });
          }
          if (answer) {
            // delete group
            await firstValueFrom(this.#api.deleteGroup(group.id, force));
            this.#alerts.printSuccess(`Group '${group.fullName}' deleted`);
            const groups = this.#state().groups.map(g => g.id === group.id ? { ...g, deleted: true } : g);
            this.#state.update(state => ({ ...state, groups }));
            await this.selectAccounts(this.#state().accounts.filter(id => !group.accounts.some(a => a.id === id)));
          }
        }
      }
    } catch (err) {
      this.#alerts.printError(err);
    }
  }

  async editAccount(account: Account) {
    return await firstValueFrom(this.#dlgService.open<Account | undefined>(
      new PolymorpheusComponent(AccEditorComponent), { data: account, dismissible: false, closeable: false, size: 's' }
    ));
  }

  async selectAccounts(ids: number[]) {
    const prev = this.#state().accounts;
    // check if the same accounts are selected
    if (ids.every(a => prev.includes(a)) && prev.every(a => ids.includes(a))) {
      return;
    }
    // check if all selected accounts are valid
    const all = computed(() => this.#state().groups.reduce((acc, g) => acc.concat(g.accounts), [] as Account[]))();
    if (ids.some(a => !all.find(aa => aa.id === a))) {
      return;
    }
    // update state
    this.#state.update(state => ({ ...state, accounts: ids }));
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
    await Promise.all([this.getTransactions(this.#state()), this.getSummary(), this.getCategoriesSummary()]);
  }

  async deselectAccounts(ids: number[]) {
    this.#state.update(state => ({ ...state, accounts: state.accounts.filter(a => !ids.includes(a)) }));
    await Promise.all([this.getTransactions(this.#state()), this.getSummary(), this.getCategoriesSummary()]);
  }

  async getCategories() {
    try {
      const categories = await firstValueFrom(this.#api.getCategories(), { defaultValue: [] });
      this.#state.update(state => ({ ...state, categories }));
    } catch (err) {
      this.#alerts.printError(err);
    }
  }

  async createCategory(id: number) {
    const parent = this.#state().categories.find(c => c.id === id);
    if (!!parent) {
      const category: Category = { id: 0, name: '', fullName: '', level: parent.level + 1, parentId: parent.id, type: parent.type };
      const data = await firstValueFrom(this.#dlgService.open<Category | undefined>(
        new PolymorpheusComponent(CatEditorComponent), { data: category, dismissible: false, closeable: false, size: 's' }
      ));
      if (!!data) {
        this.#alerts.printSuccess(`Category '${data.fullName}' created`);
        await this.getCategories();
        return data;
      }
    }
    return null;
  }

  async editCategory(id: number) {
    const category = this.#state().categories.find(c => c.id === id);
    if (!!category) {
      const data = await firstValueFrom(this.#dlgService.open<Category | undefined>(
        new PolymorpheusComponent(CatEditorComponent), { data: category, dismissible: false, closeable: false, size: 's' }
      ));
      if (!!data) {
        this.#alerts.printSuccess(`Category '${data.fullName}' updated`);
        await this.getCategories();
        return data;
      }
    }
    return null;
  }

  async deleteCategory(id: number) {
    try {
      const category = this.#state().categories.find(c => c.id === id);
      if (!!category) {
        const answer = await firstValueFrom(this.#dlgService.open<boolean>(TUI_CONFIRM, { size: 's', data: { content: 'Are you sure you want to delete this category?', yes: 'Yes', no: 'No' } }), { defaultValue: false });
        if (answer) {
          await firstValueFrom(this.#api.deleteCategory(category.id));
          this.#alerts.printSuccess(`Category '${category.fullName}' deleted`);
          if (this.#state().category?.id === id) {
            this.#state.update(state => ({ ...state, category: null }));
          }
          await this.refresh();
          return true;
        }
      }
    } catch (err) {
      this.#alerts.printError(err);
    }
    return false;
  }

  async getRules() {
    try {
      const rules = await firstValueFrom(this.#api.getRules(), { defaultValue: [] });
      this.#state.update(state => ({ ...state, rules }));
    } catch (err) {
      this.#alerts.printError(err);
    }
  }

  async editRule(rule: Rule | undefined, transaction: Partial<Transaction>) {
    const data = await firstValueFrom(this.#dlgService.open<Rule | undefined>(
      new PolymorpheusComponent(RuleEditorComponent), { data: { rule, transaction }, dismissible: false, closeable: false }
    ));
    if (!!data) {
      this.#alerts.printSuccess('Rule updated');
      this.getRules();
    }
    return data;
  }

  async deleteRule(id: number) {
    try {
      const rule = this.#state().rules.find(r => r.id === id);
      if (!!rule) {
        const answer = await firstValueFrom(this.#dlgService.open<boolean>(TUI_CONFIRM, { size: 's', data: { content: 'Are you sure you want to delete this rule?', yes: 'Yes', no: 'No' } }), { defaultValue: false });
        if (answer) {
          await firstValueFrom(this.#api.deleteRule(id));
          this.#alerts.printSuccess('Rule deleted');
          const rules = this.#state().rules.filter(r => r.id !== id);
          this.#state.update(state => ({ ...state, rules }));
          return true;
        }
      }
    } catch (err) {
      this.#alerts.printError(err);
    }
    return false;
  }

  async getSummary() {
    try {
      const state = this.#state();
      const summary = await firstValueFrom(this.#api.getSummary(state.accounts, state.range), { defaultValue: [] });
      this.#state.update(state => ({ ...state, summary }));
    } catch (err) {
      this.#alerts.printError(err);
    }
  }

  async getCategoriesSummary() {
    try {
      const state = this.#state();
      const { income, expenses } = await firstValueFrom(
        forkJoin({
          income: this.#api.getCategoriesSummary(TransactionType.Income, state.accounts, state.range),
          expenses: this.#api.getCategoriesSummary(TransactionType.Expense, state.accounts, state.range)
        })
      );
      this.#state.update(state => ({ ...state, income, expenses }));
    } catch (err) {
      this.#alerts.printError(err);
    }
  }

  async selectCategory(category: Category | null) {
    this.#state.update(state => ({ ...state, category }));
    await this.getTransactions(this.#state());
  }

  async selectCurrency(currency: string | undefined | null) {
    this.#state.update(state => ({ ...state, currency: currency || '' }));
    await this.getTransactions(this.#state());
  }

  async setSearch(search: string | undefined | null) {
    this.#state.update(state => ({ ...state, search: search || '' }));
    await this.getTransactions(this.#state());
  }

  async setRange(range: DateRange) {
    this.#state.update(state => ({ ...state, range }));
    await Promise.all([this.getTransactions(this.#state()), this.getSummary(), this.getCategoriesSummary()]);
  }

  async checkWallets(accounts: number[], fullScan: boolean) {
    try {
      const state = this.#state();
      const count = await firstValueFrom(this.#api.checkWallets(accounts, fullScan));
      if (count > 0) {
        await Promise.all([this.getTransactions(state), this.getSummary(), this.getCategoriesSummary(), this.getGroups()]);
      }
    } catch (err) {
      this.#alerts.printError(err);
    }
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
    let base = { type, opdate: date.toISOString().substring(0, 10) + ' ' + date.toTimeString().substring(0, 8), debit: 0, credit: 0, id: 0 };
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
      new PolymorpheusComponent(TrxEditorComponent), { data: transaction, dismissible: false, closeable: false, size: 's' }
    ));
    if (!!data) {
      this.#alerts.printSuccess('Transaction created');
      this.patchStateTransactions(data, false);
      if (state.categories.findIndex(c => c.id === data.category?.id) < 0) {
        await this.getCategories();
      }
      await this.getCategoriesSummary();
    }
  }

  async editTransaction() {
    const state = this.#state();
    const transaction = state.transactions.find(t => t.id === state.tid);
    if (!!transaction) {
      const data = await firstValueFrom(this.#dlgService.open<Transaction | undefined>(
        new PolymorpheusComponent(TrxEditorComponent), { data: transaction, dismissible: false, closeable: false, size: 's' }
      ));
      if (!!data) {
        this.#alerts.printSuccess('Transaction updated');
        this.patchStateTransactions(transaction, true);
        this.patchStateTransactions(data, false);
        if (state.categories.findIndex(c => c.id === data.category?.id) < 0) {
          this.getCategories();
        }
        await this.getCategoriesSummary();
      }
    }
  }

  async deleteTransaction() {
    try {
      const state = this.#state();
      const transaction = state.transactions.find(t => t.id === state.tid);
      if (!!transaction) {
        const answer = await firstValueFrom(this.#dlgService.open<boolean>(TUI_CONFIRM, { size: 's', data: { content: 'Are you sure you want to delete this transaction?', yes: 'Yes', no: 'No' } }), { defaultValue: false });
        if (answer) {
          await firstValueFrom(this.#api.deleteTransaction(transaction.id));
          this.#alerts.printSuccess('Transaction deleted');
          this.patchStateTransactions(transaction, true);
          await this.getCategoriesSummary();
        }
      }
    } catch (err) {
      this.#alerts.printError(err);
    }
  }

  async saveBackup() {
    try {
      const data = await firstValueFrom(this.#api.getBackup());
      const url = window.URL.createObjectURL(data.body as Blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `export_${new Date().toISOString().substring(0, 16)}.json`;
      link.click();
    } catch (err) {
      this.#alerts.printError(err);
    }
  }

  async loadBackup() {
    try {
      const data = await firstValueFrom(this.#dlgService.open<File>(new PolymorpheusComponent(LoadDumpComponent), { dismissible: false, closeable: false }), { defaultValue: null });
      if (!!data) {
        await firstValueFrom(this.#api.loadBackup(data));
        this.#alerts.printSuccess('Backup loaded');
        await this.refresh();
      }
    } catch (err) {
      this.#alerts.printError(err);
    }
  }

  async importTransactions(id: number) {
    try {
      const data = await firstValueFrom(this.#dlgService.open<{ bank: number, file: File }>(new PolymorpheusComponent(LoadStatComponent), { dismissible: false, closeable: false }), { defaultValue: null });
      if (!!data) {
        let transactions: TransactionImport[] | undefined = await firstValueFrom(this.#api.importTransactions(id, data.bank, data.file));
        transactions = await firstValueFrom(this.#dlgService.open<TransactionImport[] | undefined>(new PolymorpheusComponent(StatementComponent), { data: transactions, dismissible: false, closeable: false, size: 'auto' }), { defaultValue: undefined });
        if (transactions) {
          await firstValueFrom(this.#api.saveTransactions(id, transactions));
          this.#alerts.printSuccess('Transactions imported');
          await this.refresh();
        }
      }
    } catch (err) {
      this.#alerts.printError(err);
    }
  }

  patchStateTransactions(transaction: Transaction, remove: boolean) {
    const state = this.#state();
    const transactions = state.transactions.slice();
    let index = remove ? transactions.findIndex(t => t.id === transaction.id) : transactions.findIndex(t => transaction.opdate == t.opdate && (transaction.id || 0) > (t.id || 0) || transaction.opdate > t.opdate);
    if (index < 0) {
      index = transactions.length;
    }
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
    // patch group balances
    // TODO: patch groups opdate
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
    // TODO: patch categories summary
    // patch state
    this.#state.update(state => ({ ...state, transactions, groups, tid, summary }));
  }
}

function transaction2View(t: Transaction, selected: { [key: number]: boolean }): TransactionView {
  const useRecipient = t.recipient && (typeof t.account?.balance !== 'number' || typeof t.recipient?.balance === 'number' && selected[t.recipient?.id] && (!t.account || !selected[t.account?.id]));
  const amount = (t.account && !useRecipient) ? { value: t.debit, currency: t.account.currency, scale: t.account.scale } : { value: t.credit, currency: t.recipient.currency, scale: t.recipient.scale };
  const acc = useRecipient && t.recipient ? t.recipient : (t.account || t.recipient);
  return { ...t, amount, balance: { aid: acc.id, fullName: acc.fullName, currency: acc.currency, balance: acc.balance, scale: acc.scale } };
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
