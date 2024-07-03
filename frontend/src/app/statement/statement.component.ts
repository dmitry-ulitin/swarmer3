import { ChangeDetectionStrategy, Component, Inject, computed, inject } from '@angular/core';
import { TUI_TEXTFIELD_APPEARANCE_DIRECTIVE, TuiButtonModule, TuiDialogContext, TuiTextfieldControllerModule } from '@taiga-ui/core';
import { POLYMORPHEUS_CONTEXT } from '@tinkoff/ng-polymorpheus';
import { TransactionImport, TransactionType } from '../models/transaction';
import { CommonModule } from '@angular/common';
import { DataService } from '../services/data.service';
import { FormArray, FormControl, ReactiveFormsModule } from '@angular/forms';
import { Category } from '../models/category';
import { TuiDataListWrapperModule, TuiSelectModule } from '@taiga-ui/kit';
import { TuiFilterPipeModule } from '@taiga-ui/cdk';
import { ConditionType } from '../models/rule';
import { map, merge } from 'rxjs';

@Component({
  selector: 'app-statement',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, ReactiveFormsModule, TuiButtonModule, TuiSelectModule, TuiDataListWrapperModule,
    TuiFilterPipeModule, TuiTextfieldControllerModule],
  templateUrl: './statement.component.html',
  styleUrl: './statement.component.scss',
  providers: [
    {
      provide: TUI_TEXTFIELD_APPEARANCE_DIRECTIVE,
      useValue: {
        appearance: 'table',
      },
    }]
})
export class StatementComponent {
  records = this.context.data;
  #data = inject(DataService);
  categories = computed(() => this.#data.state().categories);
  readonly matcher = (category: Category, type: TransactionType): boolean => category.level > 0 && category.type == type;
  fa = new FormArray(this.records.map(r => new FormControl(r.category, { nonNullable: true })));

  constructor(@Inject(POLYMORPHEUS_CONTEXT) private readonly context: TuiDialogContext<TransactionImport[] | undefined, TransactionImport[]>) {
    merge(...this.fa.controls.map((control, index) =>
      control.valueChanges.pipe(map(value => ({ rowIndex: index, data: value })))
    )).subscribe(changes => {
      this.records[changes.rowIndex].category = changes.data;
      if (changes.data && this.records[changes.rowIndex].party) {
        this.fa.controls.forEach((control, i) => {
          const data = this.records[i];
          if (!data.category && data.type == this.records[changes.rowIndex].type &&
              this.records[changes.rowIndex].party === data.party &&
              this.records[changes.rowIndex].details === data.details) {
            data.category = changes.data;
            control.setValue(data.category, { emitEvent: false });
          }
        });
      }
    });
  }

  category(index: number) {
    return this.fa.at(index) as FormControl;
  }

  async onRule(index: number) {
    const rule = await this.#data.editRule(this.records[index].rule, this.records[index]);
    if (!!rule) {
      this.records[index].rule = rule;
      this.records[index].category = rule.category;
      this.fa.controls[index].setValue(rule.category, { emitEvent: false });
      this.fa.controls.forEach((control, i) => {
        const data = this.records[i];
        if (!data.category || rule.id == data.rule?.id) {
          data.category = undefined;
          if (!!data.party &&
            (rule.conditionType == ConditionType.PARTY_EQUALS && data.party.toLowerCase() == rule.conditionValue.toLowerCase() || rule.conditionType == ConditionType.PARTY_CONTAINS && data.party.toLowerCase().includes(rule.conditionValue.toLowerCase()))
            || !!data.details &&
            (rule.conditionType == ConditionType.DETAILS_EQUALS && data.details.toLowerCase() == rule.conditionValue.toLowerCase() || rule.conditionType == ConditionType.DETAILS_CONTAINS && data.details.toLowerCase().includes(rule.conditionValue.toLowerCase()))) {
            data.category = rule.category;
            data.rule = rule;
          }
        }
        control.setValue(data.category, { emitEvent: false });
      });
    }
  }

  onToggle(item: TransactionImport) {
    item.selected = !item.selected;
  }

  onNext() {
    this.context.completeWith(this.records);
  }

  onCancel() {
    this.context.completeWith(undefined);
  }
}
