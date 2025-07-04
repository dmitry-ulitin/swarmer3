import { TuiTextfieldControllerModule, TuiComboBoxModule, TuiInputModule, TuiInputNumberModule, TuiSelectModule } from "@taiga-ui/legacy";
import { ChangeDetectionStrategy, Component, Inject, computed, inject } from '@angular/core';
import { TuiDialogContext, TuiButton, TuiLabel } from '@taiga-ui/core';
import { POLYMORPHEUS_CONTEXT } from '@taiga-ui/polymorpheus';
import { DataService } from '../services/data.service';
import { TuiAutoFocus, TuiFilterPipe } from '@taiga-ui/cdk';
import { AlertService } from '../services/alert.service';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Account, scale } from '../models/account';

import { TuiFilterByInputPipe } from "@taiga-ui/kit";

@Component({
  selector: 'app-acc-editor',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, TuiButton, TuiInputModule, TuiFilterByInputPipe, TuiInputNumberModule, TuiComboBoxModule, TuiSelectModule, TuiTextfieldControllerModule, TuiAutoFocus, TuiLabel],
  templateUrl: './acc.editor.component.html',
  styleUrl: './acc.editor.component.scss'
})
export class AccEditorComponent {
  #data = inject(DataService);
  #alerts = inject(AlertService);
  currencies = this.#data.currencies();

  form = new FormGroup({
    id: new FormControl(this.context.data.id, { nonNullable: true }),
    name: new FormControl(this.context.data.name, { nonNullable: true }),
    fullName: new FormControl(this.context.data.fullName, { nonNullable: true }),
    currency: new FormControl(this.context.data.currency || 'EUR', { nonNullable: true, validators: [Validators.required] }),
    chain: new FormControl(this.context.data.chain || '', { nonNullable: true }),
    address: new FormControl(this.context.data.address || '', { nonNullable: true }),
    scale: new FormControl(this.context.data.scale || 2, { nonNullable: true }),
    startBalance: new FormControl(
      { value: this.context.data.startBalance || 0, disabled: !!this.context.data.id },
      { nonNullable: true, validators: [Validators.required] }
    ),
    balance: new FormControl({ value: this.context.data.balance || 0, disabled: true }, { nonNullable: true }),
    deleted: new FormControl(this.context.data.deleted || false, { nonNullable: true }),
  });

  constructor(@Inject(POLYMORPHEUS_CONTEXT) private readonly context: TuiDialogContext<Account | undefined, Account>) { }

  onCancel() {
    this.context.completeWith(undefined);
  }

  onSubmit() {
    const value = this.form.getRawValue();
    this.context.completeWith({...value, scale: scale[value.chain] || 2});
  }
}
