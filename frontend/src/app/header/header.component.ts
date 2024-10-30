import { ChangeDetectionStrategy, Component, ViewChild, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../services/auth.service';
import { TuiDialogService, TuiDataList, TuiDropdown, TuiDropdownOpen, TuiIcon, TuiButton } from '@taiga-ui/core';
import { DataService } from '../services/data.service';
import { TransactionType } from '../models/transaction';
import { Group } from '../models/group';
import { CategoriesComponent } from '../categories/categories.component';
import { PolymorpheusComponent } from '@taiga-ui/polymorpheus';
import { RulesComponent } from '../rules/rules.component';
import { TuiChevron } from '@taiga-ui/kit';

@Component({
  selector: 'app-header',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, TuiButton, TuiDropdown, TuiDataList, TuiIcon, TuiChevron],
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss'
})
export class HeaderComponent {
  auth = inject(AuthService);
  data = inject(DataService);
  account = computed(() => this.data.selectedAccount());
  #dlgService = inject(TuiDialogService);
  group = computed(() => this.data.selectedGroup());

  @ViewChild('userMenu') userMenu?: TuiDropdownOpen;

  onLogout() {
    this.userMenu?.toggle(false);
    this.auth.logout();
  }

  onExpense() {
    this.data.createTransaction(TransactionType.Expense);
  }

  onTransfer() {
    this.data.createTransaction(TransactionType.Transfer);
  }

  onIncome() {
    this.data.createTransaction(TransactionType.Income);
  }

  onRefresh() {
    this.data.refresh();
  }

  editTransaction() {
    this.data.editTransaction();
  }

  deleteTransaction() {
    this.data.deleteTransaction();
  }

  editGroup(group: Group) {
    this.data.editGroup(group.id);
  }

  deleteGroup(group: Group) {
    this.data.deleteGroup(group.id);
  }

  onCategories() {
    this.#dlgService.open(new PolymorpheusComponent(CategoriesComponent), { header: "Categories", size: 'l' }).subscribe();
  }

  onRules() {
    this.#dlgService.open(new PolymorpheusComponent(RulesComponent), { header: "Rules", size: 'auto' }).subscribe();
  }

  onSaveBackup() {
    this.data.saveBackup();
  }

  onLoadBackup() {
    this.data.loadBackup();
  }

  onImport(id: number) {
    this.data.importTransactions(id);
  }
}
