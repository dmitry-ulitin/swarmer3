import { ChangeDetectionStrategy, Component, Inject, computed, inject } from '@angular/core';
import { Rule } from '../models/rule';
import { TuiButtonModule, TuiDialogContext, TuiTextfieldControllerModule } from '@taiga-ui/core';
import { POLYMORPHEUS_CONTEXT } from '@tinkoff/ng-polymorpheus';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Category } from '../models/category';
import { DataService } from '../services/data.service';
import { Transaction } from '../models/transaction';
import { TuiDataListWrapperModule, TuiInputModule, TuiSelectModule } from '@taiga-ui/kit';
import { TuiFilterPipeModule } from '@taiga-ui/cdk';
import { ApiService } from '../services/api.service';
import { AlertService } from '../services/alert.service';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'app-rule',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TuiButtonModule, ReactiveFormsModule, TuiInputModule, TuiSelectModule, TuiTextfieldControllerModule, TuiDataListWrapperModule, TuiFilterPipeModule],
  templateUrl: './rule.component.html',
  styleUrl: './rule.component.scss'
})
export class RuleComponent {
  #data = inject(DataService);
  #api = inject(ApiService);
  #alerts = inject(AlertService);
  transaction = this.context.data.transaction;
  rule = this.context.data.rule ||  { conditionType: 1, conditionValue: this.transaction.party} ;
  categories = computed(() => this.#data.state().categories);
  readonly matcher = (category: Category): boolean => category.level > 0 && category.type == this.transaction.type;
  fields = [{ id: 1, name: 'party' }, { id: 2, name: 'details' }];
  conditions = [{ id: 1, name: 'equals' }, { id: 2, name: 'contains' }];
  form = new FormGroup({
    id: new FormControl(this.context.data.rule?.id, { nonNullable: true }),
    field: new FormControl(this.rule.conditionType < 3 ? this.fields[0] : this.fields[1], { nonNullable: true, validators: [Validators.required] }),
    condition: new FormControl(this.rule.conditionType % 2 ? this.conditions[0] : this.conditions[1], { nonNullable: true, validators: [Validators.required] }),
    conditionValue: new FormControl(this.rule.conditionValue, { nonNullable: true, validators: [Validators.required] }),
    category: new FormControl(this.context.data.rule?.category, { nonNullable: true, validators: [Validators.required] })
  });

  constructor(@Inject(POLYMORPHEUS_CONTEXT) private readonly context: TuiDialogContext<Rule | undefined, { rule?: Rule, transaction: Partial<Transaction> }>) {
    this.form.controls['field'].valueChanges.subscribe((v) => {
      if (v?.id == 1 && this.transaction.party) {
        this.form.controls['conditionValue'].setValue(this.transaction.party);
      }
      else if (v?.id == 2 && this.transaction.details) {
        this.form.controls['conditionValue'].setValue(this.transaction.details);
      }
    });
  }

  onCancel() {
    this.context.completeWith(undefined);
  }

  async onSubmit() {
    try {
      const value = this.form.getRawValue();
      const conditionType = this.form.value.field?.id == 1 ? (this.form.value.condition?.id == 1 ? 1 : 2) : (this.form.value.condition?.id == 1 ? 3 : 4);
      if (!!value.category && !!value.conditionValue) {
        const rule = await firstValueFrom(this.#api.saveRule({ id: value.id, category: value.category, conditionValue: value.conditionValue, conditionType }));
        this.context.completeWith(rule);
      }
    } catch (error) {
      this.#alerts.printError(error);
    }
  }
}
