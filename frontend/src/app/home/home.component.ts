import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AccountsComponent } from '../accounts/accounts.component';
import { TransactionsComponent } from '../transactions/transactions.component';
import { FiltersComponent } from '../filters/filters.component';
import { CatSummaryComponent } from '../reports/cat.summary.component';
import { TypeSummaryComponent } from '../reports/type.summary.component';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, FiltersComponent, AccountsComponent, TransactionsComponent, TypeSummaryComponent, CatSummaryComponent],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss'
})
export class HomeComponent {

}
