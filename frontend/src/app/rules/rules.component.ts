import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { DataService } from '../services/data.service';
import { ConditionType } from '../models/rule';
import { TuiButtonModule } from '@taiga-ui/core';
import { TransactionType } from '../models/transaction';

@Component({
  selector: 'app-rules',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TuiButtonModule],
  templateUrl: './rules.component.html',
  styleUrl: './rules.component.scss'
})
export class RulesComponent {
  #data = inject(DataService);
  rules = computed(() => this.#data.state().rules.sort((a, b) => a.category.fullname.localeCompare(b.category.fullname)));
  rid = -1;
  ct2str = (ct: ConditionType) => { return ['party', 'details','category name'][(ct - ct % 2) / 2] + [' contains', ' equals'][ct % 2]; }

  onSelect(id?: number) {
    this.rid = id ?? -1;
  }

  async onDelete() {
    if (await this.#data.deleteRule(this.rid)) {
      this.rid = -1;
    } 
  }

  async onAddInc() {
    this.rid = (await this.#data.editRule(undefined, {type: TransactionType.Income}))?.id || this.rid;
  }

  async onAddEx() {
    this.rid = (await this.#data.editRule(undefined, {type: TransactionType.Expense}))?.id || this.rid;
  }

  onEdit() {
    const rule = this.#data.state().rules.find(r => r.id === this.rid);
    if (rule) {
      this.#data.editRule(rule, {type: rule.category.type});
    }
  }
}
