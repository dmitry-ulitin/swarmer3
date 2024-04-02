import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AccountsComponent } from '../accounts/accounts.component';
import { TransactionsComponent } from '../transactions/transactions.component';
import { FiltersComponent } from '../filters/filters.component';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, FiltersComponent, AccountsComponent, TransactionsComponent],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss'
})
export class HomeComponent {

}
