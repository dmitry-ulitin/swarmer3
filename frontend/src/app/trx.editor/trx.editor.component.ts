import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { TuiTextfieldControllerModule, TuiTextareaModule, TuiComboBoxModule, TuiInputModule, TuiInputDateModule, TuiInputNumberModule, TuiSelectModule } from "@taiga-ui/legacy";
import { ChangeDetectionStrategy, Component, Inject, computed, inject } from '@angular/core';
import { POLYMORPHEUS_CONTEXT } from '@taiga-ui/polymorpheus';
import { Transaction, TransactionType } from '../models/transaction';
import { TuiDialogContext, TuiButton, TuiLabel } from '@taiga-ui/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TuiDataListWrapper, TuiFilterByInputPipe } from '@taiga-ui/kit';
import { DataService } from '../services/data.service';
import { TuiDay, TuiAutoFocus, TuiFilterPipe } from '@taiga-ui/cdk';
import { Category } from '../models/category';
import { firstValueFrom, startWith, takeUntil } from 'rxjs';
import { ApiService } from '../services/api.service';
import { AlertService } from '../services/alert.service';

@Component({
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TuiButton, ReactiveFormsModule, TuiInputModule, TuiInputNumberModule, TuiLabel,
    TuiTextfieldControllerModule, TuiComboBoxModule, TuiSelectModule, TuiDataListWrapper,
    TuiTextareaModule, TuiInputDateModule, TuiFilterPipe, TuiFilterByInputPipe, TuiAutoFocus],
  templateUrl: './trx.editor.component.html',
  styleUrl: './trx.editor.component.scss'
})
export class TrxEditorComponent {
  #data = inject(DataService);
  #api = inject(ApiService);
  #alerts = inject(AlertService);
  currencies = this.#data.currencies();
  accounts = this.#data.allAccounts();
  account = this.context.data.account;
  recipient = this.context.data.recipient;
  categories = computed(() => this.#data.state().categories);
  readonly matcher = (category: Category, type: TransactionType): boolean => category.level > 0 && category.type == type;
  newcategory = false;
  timepart = this.context.data.opdate.substring(11, 19);

  form = new FormGroup({
    id: new FormControl(this.context.data.id, { nonNullable: true }),
    opdate: new FormControl(TuiDay.fromLocalNativeDate(new Date(this.context.data.opdate)), { nonNullable: true, validators: [Validators.required] }),
    account: new FormControl(this.context.data.account),
    credit: new FormControl(this.context.data.credit ? this.context.data.credit : undefined, { nonNullable: true }),
    ccurrency: new FormControl(this.context.data.recipient?.currency || this.context.data.currency || this.context.data.account?.currency, { nonNullable: true }),
    recipient: new FormControl(this.context.data.recipient),
    debit: new FormControl(this.context.data.debit ? this.context.data.debit : undefined, { nonNullable: true }),
    dcurrency: new FormControl(this.context.data.account?.currency || this.context.data.currency || this.context.data.recipient?.currency, { nonNullable: true }),
    category: new FormControl(this.context.data.category),
    newcategory: new FormControl(''),
    details: new FormControl(this.context.data.details, { nonNullable: true }),
    party: new FormControl(this.context.data.party, { nonNullable: true }),
    type: new FormControl(this.context.data.type, { nonNullable: true, validators: [Validators.required] })
  });

  get type(): TransactionType {
    return this.form.controls['type'].value;
  }

  get category(): Category | undefined | null {
    return this.form.controls['category'].value;
  }

  get typeString(): string {
    return TransactionType[this.type].toLocaleLowerCase();
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

  get showTransfer(): boolean {
    return (this.type == TransactionType.Income || this.type == TransactionType.Expense) && this.accounts.length > 1;
  }

  get showIncome(): boolean {
    return this.type === TransactionType.Transfer && (this.category?.type === TransactionType.Income || !this.category);
  }

  get showExpense(): boolean {
    return this.type === TransactionType.Transfer && (this.category?.type === TransactionType.Expense || !this.category);
  }

  get categoryParent(): string {
    const category = this.form.controls['category'].value;
    return category == null ? '' : (category.fullname + ' / ');
  }

  constructor(@Inject(POLYMORPHEUS_CONTEXT) private readonly context: TuiDialogContext<Transaction | undefined, Transaction>) {
    this.form.controls['account'].valueChanges.pipe(takeUntilDestroyed()).subscribe(account => {
      if (account) {
        this.form.controls['dcurrency'].setValue(account.currency);
        if (this.type !== TransactionType.Transfer) {
          this.form.controls['ccurrency'].setValue(account.currency);
        }
        if (this.type === TransactionType.Transfer && account.id === this.recipient?.id) {
          this.form.controls['recipient'].setValue(this.account, { emitEvent: false });
          this.recipient = this.account;
        }
        if (this.type === TransactionType.Correction) {
          this.form.controls['credit'].setValue(0);
          this.form.controls['debit'].setValue(account.balance);
        }
      }
      this.account = account;
    });
    this.form.controls['recipient'].valueChanges.pipe(takeUntilDestroyed()).subscribe(recipient => {
      if (recipient) {
        this.form.controls['ccurrency'].setValue(recipient.currency);
        if (this.type !== TransactionType.Transfer) {
          this.form.controls['dcurrency'].setValue(recipient.currency);
        }
        if (this.type === TransactionType.Transfer && recipient.id === this.account?.id) {
          this.form.controls['account'].setValue(this.recipient, { emitEvent: false });
          this.account = this.recipient;
        }
      }
      this.recipient = recipient;
    });
    this.form.controls['type'].valueChanges.pipe(takeUntilDestroyed(), startWith(context.data.type)).subscribe(type => {
      let category = this.form.controls['category'].value;
      if (type === TransactionType.Expense) {
        this.form.controls['ccurrency'].enable();
        this.form.controls['dcurrency'].disable();
        if (type !== category?.type) {
          this.form.controls['category'].setValue(null, { emitEvent: false });
        }
      } else if (type === TransactionType.Income) {
        this.form.controls['ccurrency'].disable();
        this.form.controls['dcurrency'].enable();
        if (type !== category?.type) {
          this.form.controls['category'].setValue(null, { emitEvent: false });
        }
      } else {
        this.form.controls['ccurrency'].disable();
        this.form.controls['dcurrency'].disable();
        if (type === TransactionType.Transfer) {
          let account = this.form.controls['account'].value;
          let recipient = this.form.controls['recipient'].value;
          if (!account || !recipient) {
            const transactions = computed(() => this.#data.state().transactions)();
            const transfer = transactions.find(t => t.type === TransactionType.Transfer && (t.account?.id === account?.id || t.recipient?.id === recipient?.id));
            if (recipient) {
              account = transfer?.account || this.accounts.find(a => a.id !== recipient?.id && a.currency === recipient?.currency) ||
                this.accounts.find(a => a.id !== recipient?.id) || null;
              this.form.controls['account'].setValue(account, { emitEvent: false });
            }
            if (account) {
              recipient = transfer?.recipient || this.accounts.find(a => a.id !== account?.id && a.currency === account?.currency) ||
                this.accounts.find(a => a.id !== account?.id) || null;
              this.form.controls['recipient'].setValue(recipient, { emitEvent: false });
            }
            this.form.controls['dcurrency'].setValue(account?.currency, { emitEvent: false });
            this.form.controls['ccurrency'].setValue(recipient?.currency, { emitEvent: false });
          }
        }
      }
    });
  }

  onYesterday(): void {
    const opdate = this.form.controls['opdate'].value;
    this.form.controls['opdate'].setValue(opdate.append({ day: -1 }));
  }

  onToday(): void {
    this.form.controls['opdate'].setValue(TuiDay.currentLocal());
  }

  onTomorrow(): void {
    const opdate = this.form.controls['opdate'].value;
    this.form.controls['opdate'].setValue(opdate.append({ day: 1 }));
  }

  onCreateCategory() {
    this.newcategory = true;
    this.form.controls['newcategory'].setValue('');
  }

  onCancelCategory() {
    this.newcategory = false;
    this.form.controls['newcategory'].setValue(null);
  }

  onTransfer() {
    this.form.controls['type'].setValue(TransactionType.Transfer);
  }

  onIncome() {
    this.form.controls['type'].setValue(TransactionType.Income);
  }

  onExpense() {
    this.form.controls['type'].setValue(TransactionType.Expense);
  }

  onCancel() {
    this.context.completeWith(undefined);
  }

  async onSubmit() {
    try {
      const value = this.form.getRawValue();
      const debit = (this.showDebit ? value.debit : value.credit) || 0;
      const credit = (this.showCredit ? value.credit : value.debit) || 0;
      if (debit == 0 && credit == 0) {
        return;
      }
      const opdate = value.opdate.getFormattedDay('YMD', '-') + ' ' + this.timepart;
      if (this.newcategory && !!value.newcategory) {
        let parent = value.category;
        value.category = { id: 0, name: value.newcategory, fullname: (parent ? (parent.fullname + ' / ') : '') + value.newcategory, level: parent ? parent.level + 1 : 1, type: value.type, parent_id: parent ? parent.id : value.type }
      }

      let transaction: Transaction | undefined = undefined;
      if (value.type == TransactionType.Income && !!value.recipient) {
        transaction = { ...value, opdate, currency: value.dcurrency?.toUpperCase(), debit, credit, account: null, recipient: value.recipient };
      } else if (value.type == TransactionType.Expense && !!value.account) {
        transaction = { ...value, opdate, currency: value.ccurrency?.toUpperCase(), debit, credit, account: value.account, recipient: null };
      } else if (value.type == TransactionType.Correction && !!value.account) {
        if (credit < 0) {
          transaction = { ...value, opdate, currency: value.account.currency, debit: -credit, credit: -credit, account: value.account, recipient: null };
        } else {
          transaction = { ...value, opdate, currency: value.account.currency, debit: credit, credit, account: null, recipient: value.account };
        }
      } else if (value.type == TransactionType.Transfer && !!value.account && !!value.recipient) {
        transaction = { ...value, opdate, currency: undefined, debit, credit, account: value.account, recipient: value.recipient, category: undefined };
      }
      if (!!transaction) {
        transaction = await firstValueFrom(this.#api.saveTransaction(transaction));
      }
      this.context.completeWith(transaction);
    } catch (error) {
      this.#alerts.printError(error);
    }
  }
}