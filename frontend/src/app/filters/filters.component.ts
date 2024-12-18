import { TuiTextfieldControllerModule, TuiInputModule, TuiTagModule } from "@taiga-ui/legacy";
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { DataService } from '../services/data.service';
import { debounceTime } from 'rxjs';
import { RangeComponent } from './range/range.component';

@Component({
  selector: 'app-filters',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, TuiInputModule, TuiTextfieldControllerModule, TuiTagModule, RangeComponent],
  templateUrl: './filters.component.html',
  styleUrl: './filters.component.scss'
})
export class FiltersComponent {
  data = inject(DataService);
  search = new FormControl('');

  constructor() {
    this.search.valueChanges.pipe(debounceTime(500)).subscribe(value => this.data.setSearch(value));
  }

  removeFilter(f: { name: string; ids: number[]; }) {
    this.data.deselectAccounts(f.ids);
  }

  removeCategory() {
    this.data.selectCategory(null);
  }

  removeCurrency() {
    this.data.selectCurrency(null);
  }
}
