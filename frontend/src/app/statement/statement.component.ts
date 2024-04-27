import { Component, Inject, computed, inject } from '@angular/core';
import { TUI_TEXTFIELD_APPEARANCE_DIRECTIVE, TuiButtonModule, TuiDialogContext, TuiTextfieldControllerModule } from '@taiga-ui/core';
import { POLYMORPHEUS_CONTEXT } from '@tinkoff/ng-polymorpheus';
import { TransactionImport, TransactionType } from '../models/transaction';
import { CommonModule } from '@angular/common';
import { DataService } from '../services/data.service';
import { FormArray, FormControl, ReactiveFormsModule } from '@angular/forms';
import { Category } from '../models/category';
import { TuiDataListWrapperModule, TuiSelectModule } from '@taiga-ui/kit';
import { TuiFilterPipeModule } from '@taiga-ui/cdk';


@Component({
  selector: 'app-statement',
  standalone: true,
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
  constructor(@Inject(POLYMORPHEUS_CONTEXT) private readonly context: TuiDialogContext<TransactionImport[] | undefined, TransactionImport[]>) {}

  category(index: number) {
    return this.fa.at(index) as FormControl;
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
