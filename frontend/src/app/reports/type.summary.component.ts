import { CommonModule } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { TuiLinkModule } from '@taiga-ui/core';
import { DataService } from '../services/data.service';
import { TransactionType } from '../models/transaction';

@Component({
  selector: 'app-type-summary',
  standalone: true,
  imports: [CommonModule, TuiLinkModule],
  templateUrl: './type.summary.component.html',
  styleUrl: './type.summary.component.scss'
})
export class TypeSummaryComponent {
  #data = inject(DataService);
  income = computed(() => this.#data.state().summary.filter(s => s.credit));
  expenses = computed(() => this.#data.state().summary.filter(s => s.debit));
  transfers = computed(() => this.#data.state().summary.filter(s => (s.transfers_credit-s.transfers_debit)));

  onCurrency(currency: string) {
    this.#data.selectCurrency(currency);
  }

  onExpenses() {
    this.#data.selectCategory({id: TransactionType.Expense, name: "Expenses", fullname: "Expenses", level: 0, type: TransactionType.Expense, parent_id: null});
  }

  onIncome() {
    this.#data.selectCategory({id: TransactionType.Income, name: "Income", fullname: "Income", level: 0, type: TransactionType.Income, parent_id: null});
  }

  onTransfers() {
    this.#data.selectCategory({id: TransactionType.Transfer, name: "Transfers", fullname: "Transfers", level: 0, type: TransactionType.Transfer, parent_id: null});
  }
}
