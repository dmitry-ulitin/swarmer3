import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { DataService } from '../services/data.service';
import { InfiniteScrollModule } from 'ngx-infinite-scroll';
import { CommonModule } from '@angular/common';
import { TuiLinkModule } from '@taiga-ui/core';
import { Category } from '../models/category';

@Component({
  selector: 'app-transactions',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, InfiniteScrollModule, TuiLinkModule],
  templateUrl: './transactions.component.html',
  styleUrl: './transactions.component.scss'
})
export class TransactionsComponent {
  data = inject(DataService);
  transactions = computed(() => this.data.state().transactions);
  tid = computed(() => this.data.state().tid);

  selectTransaction(id: number) {
    this.data.selectTransaction(id);
  }

  onScroll(): void {
    this.data.scrollTransactions();
  }

  selectAccount(aid: number, event: MouseEvent): void {
    event.stopPropagation();
    this.data.selectAccounts([aid]);
  }

  selectCategory(category: Category | null, event: MouseEvent) {
    event.stopPropagation();
    this.data.selectCategory(category);
  }
}
