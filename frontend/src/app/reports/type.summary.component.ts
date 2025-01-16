import { TuiLink } from "@taiga-ui/core";
import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { DataService } from '../services/data.service';
import { TransactionType } from '../models/transaction';

@Component({
  selector: 'app-type-summary',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, TuiLink],
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
    this.#data.selectCategory({id: TransactionType.Expense, name: "Expenses", fullName: "Expenses", level: 0, type: TransactionType.Expense, parentId: null});
  }

  onIncome() {
    this.#data.selectCategory({id: TransactionType.Income, name: "Income", fullName: "Income", level: 0, type: TransactionType.Income, parentId: null});
  }

  onTransfers() {
    this.#data.selectCategory({id: TransactionType.Transfer, name: "Transfers", fullName: "Transfers", level: 0, type: TransactionType.Transfer, parentId: null});
  }
}
