import { Component, inject } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { TuiTextfieldControllerModule } from '@taiga-ui/core';
import { TuiInputModule, TuiTagModule } from '@taiga-ui/kit';
import { DataService } from '../services/data.service';
import { debounceTime } from 'rxjs';

@Component({
  selector: 'app-filters',
  standalone: true,
  imports: [ReactiveFormsModule, TuiInputModule, TuiTextfieldControllerModule, TuiTagModule],
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
