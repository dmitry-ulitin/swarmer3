import { ChangeDetectionStrategy, Component, Inject, computed, inject } from '@angular/core';
import { Rule } from '../models/rule';
import { TuiButtonModule, TuiDialogContext, TuiTextfieldControllerModule } from '@taiga-ui/core';
import { POLYMORPHEUS_CONTEXT } from '@tinkoff/ng-polymorpheus';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Category } from '../models/category';
import { DataService } from '../services/data.service';
import { TransactionImport } from '../models/transaction';
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
  rule = this.context.data.rule ||  { conditionType: (!!this.transaction.party ? 1 : (!!this.transaction.catname ? 5 : 3)), conditionValue: this.transaction.party || this.transaction.catname || this.transaction.details, category: null }; ;
  categories = computed(() => this.#data.state().categories);
  readonly matcher = (category: Category): boolean => category.level > 0 && category.type == this.transaction.type;
  fields = [{ id: 1, name: 'party' }, { id: 3, name: 'details' }, { id: 5, name: 'cat.name' }];
  conditions = [{ id: 0, name: 'equals' }, { id: 1, name: 'contains' }];
  form = new FormGroup({
    id: new FormControl(this.context.data.rule?.id, { nonNullable: true }),
    field: new FormControl(this.fields[(this.rule.conditionType - 1) / 2], { nonNullable: true, validators: [Validators.required] }),
    condition: new FormControl(this.conditions[this.rule.conditionType % 2], { nonNullable: true, validators: [Validators.required] }),
    conditionValue: new FormControl(this.rule.conditionValue, { nonNullable: true, validators: [Validators.required] }),
    category: new FormControl(this.context.data.rule?.category, { nonNullable: true, validators: [Validators.required] })
  });

  constructor(@Inject(POLYMORPHEUS_CONTEXT) private readonly context: TuiDialogContext<Rule | undefined, { rule?: Rule, transaction: Partial<TransactionImport> }>) {
    this.form.controls['field'].valueChanges.subscribe((v) => {
      if (v.id == 1 && this.transaction.party) {
        this.form.controls['conditionValue'].setValue(this.transaction.party);
      }
      else if (v.id == 3 && this.transaction.details) {
        this.form.controls['conditionValue'].setValue(this.transaction.details);
      }
      else if (v.id == 5 && this.transaction.catname) {
        this.form.controls['conditionValue'].setValue(this.transaction.catname);
      }
    });
  }

  onCancel() {
    this.context.completeWith(undefined);
  }

  async onSubmit() {
    try {
      const value = this.form.getRawValue();
      const conditionType = value.field.id  + value.condition.id;
      if (!!value.category && !!value.conditionValue) {
        const rule = await firstValueFrom(this.#api.saveRule({ id: value.id, category: value.category, conditionValue: value.conditionValue, conditionType }));
        this.context.completeWith(rule);
      }
    } catch (error) {
      this.#alerts.printError(error);
    }
  }
}
