import { Component, Inject } from '@angular/core';
import { POLYMORPHEUS_CONTEXT } from '@tinkoff/ng-polymorpheus';
import { Transaction, TransactionType } from '../models/transaction';
import { TuiButtonModule, TuiDialogContext, TuiLabelModule, TuiTextfieldControllerModule } from '@taiga-ui/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TuiInputModule, TuiComboBoxModule, TuiDataListWrapperModule, TuiFilterByInputPipeModule, TuiInputNumberModule } from '@taiga-ui/kit';

@Component({
  selector: 'app-trx.editor',
  standalone: true,
  imports: [TuiButtonModule, ReactiveFormsModule, TuiInputModule, TuiInputNumberModule, TuiLabelModule, TuiTextfieldControllerModule, TuiComboBoxModule, TuiDataListWrapperModule, TuiFilterByInputPipeModule],
  templateUrl: './trx.editor.component.html',
  styleUrl: './trx.editor.component.scss'
})
export class TrxEditorComponent {

  form = new FormGroup({
    id: new FormControl(this.context.data.id),
    opdate: new FormControl('', Validators.required),
    account: new FormControl(this.context.data.account),
    credit: new FormControl(this.context.data.credit),
    ccurrency: new FormControl(this.context.data.currency || this.context.data.account?.currency),
    recipient: new FormControl(this.context.data.recipient),
    debit: new FormControl(this.context.data.debit),
    dcurrency: new FormControl(this.context.data.currency || this.context.data.recipient?.currency),
    category: new FormControl(this.context.data.category),
    details: new FormControl(this.context.data.details),
    party: new FormControl(this.context.data.party),
    type: new FormControl(this.context.data.type, { nonNullable: true, validators: [Validators.required] })
  });

  get type(): TransactionType {
    return this.form.controls['type'].value;
  }

  get convertation(): boolean {
    const dcurrency = this.form.controls['dcurrency'].value;
    const ccurrency = this.form.controls['ccurrency'].value;
    return !!dcurrency && !!ccurrency && dcurrency !== ccurrency;
  }

  get showCredit(): boolean {
    return this.convertation || this.type === TransactionType.Expense || this.type === TransactionType.Correction;
  }

  get showDebit(): boolean {
    return this.convertation || this.type !== TransactionType.Expense;
  }

  get showAccount(): boolean {
    return this.type === TransactionType.Expense || this.type === TransactionType.Transfer || (this.type === TransactionType.Correction && !!this.form.controls['account'].value);
  }

  get showRecipient(): boolean {
    return this.type === TransactionType.Income || this.type === TransactionType.Transfer || (this.type === TransactionType.Correction && !!this.form.controls['recipient'].value);
  }

  get showCategory(): boolean {
    return this.type === TransactionType.Income || this.type === TransactionType.Expense;
  }

  get categoryParent(): string {
    const category = this.form.controls['category'].value;
    return category == null ? '' : (category.fullname + ' / ');
  }

  constructor(@Inject(POLYMORPHEUS_CONTEXT) private readonly context: TuiDialogContext<Transaction | undefined, Transaction>) {}

  onSubmit() {
    this.context.completeWith(undefined);
  }

  onCancel() {
    this.context.completeWith(undefined);
  }
}
