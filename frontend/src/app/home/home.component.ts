import { ChangeDetectionStrategy, Component } from '@angular/core';

import { AccountsComponent } from '../accounts/accounts.component';
import { TransactionsComponent } from '../transactions/transactions.component';
import { FiltersComponent } from '../filters/filters.component';
import { CatSummaryComponent } from '../reports/cat.summary.component';
import { TypeSummaryComponent } from '../reports/type.summary.component';

@Component({
  selector: 'app-home',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FiltersComponent, AccountsComponent, TransactionsComponent, TypeSummaryComponent, CatSummaryComponent],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss'
})
export class HomeComponent {

}
