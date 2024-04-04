import { Component, ViewChild, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../services/auth.service';
import { TuiButtonModule, TuiDataListModule, TuiHostedDropdownComponent, TuiHostedDropdownModule, TuiModeModule, TuiSvgModule } from '@taiga-ui/core';
import { DataService } from '../services/data.service';
import { TransactionType } from '../models/transaction';
import { Group } from '../models/group';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, TuiButtonModule, TuiModeModule, TuiHostedDropdownModule, TuiDataListModule, TuiSvgModule],
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss'
})
export class HeaderComponent {
  auth = inject(AuthService);
  data = inject(DataService);
  group = computed(() => this.data.selectedGroup());

  @ViewChild('userMenu') userMenu?: TuiHostedDropdownComponent;

  onLogout() {
    this.userMenu?.close();
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

  createGroup() {
    this.data.createGroup();
  }

  editGroup(group: Group) {
    this.data.editGroup(group.id);
  }

  deleteGroup(group: Group) {
    this.data.deleteGroup(group.id);
  }

  onCategories() {}

  onSaveBackup() {}

  onLoadBackup() {}
}
