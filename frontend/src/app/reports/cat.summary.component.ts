import { TuiLink } from "@taiga-ui/core";
import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { DataService } from '../services/data.service';
import { Category } from '../models/category';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-cat-summary',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, TuiLink],
  templateUrl: './cat.summary.component.html',
  styleUrl: './cat.summary.component.scss'
})
export class CatSummaryComponent {
  #data = inject(DataService);
  income = computed(() => {
    const state = this.#data.state();
    return [...state.income.filter(s => !state.currency || state.currency === s.currency).reduce((a, i) => {
      const key = i.category?.id || 0;
      const s = a.get(key) || { category: { ...i.category, id: key < 3 ? -key : key, fullName: key < 3 ? "No Category" : i.category?.fullName }, amounts: [] };
      s.amounts.push({ value: i.amount, currency: i.currency });
      a.set(key, s);
      return a;
    }, new Map<number, { category: Category, amounts: { value: number, currency: string }[] }>()).values()];
  });
  expenses = computed(() => {
    const state = this.#data.state();
    return [...state.expenses.filter(s => !state.currency || state.currency === s.currency).reduce((a, e) => {
      const key = e.category?.id || 0;
      const s = a.get(key) || { category: { ...e.category, id: key < 3 ? -key : key, fullName: key < 3 ? "No Category" : e.category?.fullName }, amounts: [] };
      s.amounts.push({ value: e.amount, currency: e.currency });
      a.set(key, s);
      return a;
    }, new Map<number, { category: Category, amounts: { value: number, currency: string }[] }>()).values()];
  });

  setCategory(category: Category) {
    this.#data.selectCategory(category);
  }
}
