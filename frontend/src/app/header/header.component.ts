import { Component, ViewChild, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../services/auth.service';
import { TuiButtonModule, TuiDataListModule, TuiHostedDropdownComponent, TuiHostedDropdownModule, TuiModeModule, TuiSvgModule } from '@taiga-ui/core';
import { DataService } from '../services/data.service';
import { TransactionType } from '../models/transaction';

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
}
