import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { TuiTextfieldControllerModule, TuiComboBoxModule, TuiInputModule, TuiInputNumberModule, TuiSelectModule } from "@taiga-ui/legacy";
import { ChangeDetectionStrategy, Component, Inject, inject } from '@angular/core';
import { TuiDialogContext, TuiDataList, TuiButton } from '@taiga-ui/core';
import { Group, Permission } from '../models/group';
import { POLYMORPHEUS_CONTEXT } from '@taiga-ui/polymorpheus';
import { DataService } from '../services/data.service';
import { TuiAutoFocus, TuiLet } from '@taiga-ui/cdk';
import { ApiService } from '../services/api.service';
import { AlertService } from '../services/alert.service';
import { FormArray, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { debounceTime, firstValueFrom, map, of, switchMap, tap } from 'rxjs';
import { Account, scale } from '../models/account';
import { TuiDataListWrapper, TuiFilterByInputPipe } from '@taiga-ui/kit';
import { AuthService } from '../services/auth.service';
import { CommonModule } from '@angular/common';
import { AccEditorComponent } from "../acc.editor/acc.editor.component";

@Component({
  selector: 'app-grp-editor',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, ReactiveFormsModule, TuiButton, TuiInputModule, TuiInputNumberModule, TuiComboBoxModule, TuiSelectModule,
    TuiTextfieldControllerModule, TuiDataList, TuiDataListWrapper, TuiFilterByInputPipe, TuiLet, TuiAutoFocus],
  templateUrl: './grp.editor.component.html',
  styleUrl: './grp.editor.component.scss'
})
export class GrpEditorComponent {
  #data = inject(DataService);
  #api = inject(ApiService);
  #alerts = inject(AlertService);
  #auth = inject(AuthService);
  email = this.#auth.claims().sub;
  currencies = this.#data.currencies();
  options = [{ id: 0, name: 'read' }, { id: 1, name: 'write' }, { id: 3, name: 'admin' }];
  rights = new FormControl(this.options[0]);
  query = new FormControl('');
  selected = '';
  users$ = this.query.valueChanges.pipe(tap(v => this.selected = ''), debounceTime(500), switchMap(q => !!q && q.length > 2 ? this.#api.getUsers(q || '') : of([])),
    map(a => a.filter(u => u !== this.form.value.ownerEmail && !this.permissions.controls.find(p => p.value.user.email === u))));

  acc2form = (acc: Account) => new FormGroup({
    id: new FormControl(acc.id, { nonNullable: true }),
    name: new FormControl(acc.name, { nonNullable: true }),
    fullName: new FormControl(acc.fullName, { nonNullable: true }),
    currency: new FormControl({ value: acc.currency || this.#auth.claims().currency || 'EUR', disabled: !!acc.opdate }, { nonNullable: true, validators: [Validators.required] }),
    chain: new FormControl(acc.chain, { nonNullable: true }),
    address: new FormControl(acc.address, { nonNullable: true }),
    scale: new FormControl(acc.scale || 2, { nonNullable: true }),
    startBalance: new FormControl({ value: acc.startBalance || 0, disabled: !!acc.opdate }, { nonNullable: true, validators: [Validators.required] }),
    balance: new FormControl({ value: acc.balance || 0, disabled: true }, { nonNullable: true }),
    deleted: new FormControl(acc.deleted, { nonNullable: true }),
    opdate: new FormControl(acc.opdate, { nonNullable: true })
  });

  user2form = (p: Permission) => new FormGroup({
    user: new FormControl(p.user, { nonNullable: true }),
    readonly: new FormControl(p.readonly && !p.admin, { nonNullable: true }),
    write: new FormControl({ value: !p.readonly || p.admin, disabled: !this.isOwnerOrCoowner }, { nonNullable: true }),
    admin: new FormControl({ value: p.admin, disabled: !this.isOwnerOrCoowner }, { nonNullable: true })
  });

  form = new FormGroup({
    id: new FormControl(this.context.data.id, { nonNullable: true }),
    fullName: new FormControl(this.context.data.fullName, { nonNullable: true, validators: [Validators.required] }),
    owner: new FormControl(this.context.data.owner, { nonNullable: true }),
    coowner: new FormControl(this.context.data.coowner, { nonNullable: true }),
    shared: new FormControl(this.context.data.shared, { nonNullable: true }),
    accounts: new FormArray(this.context.data.accounts.map(this.acc2form)),
    permissions: new FormArray(this.context.data.permissions.map(this.user2form)),
    ownerEmail: new FormControl(this.context.data.ownerEmail, { nonNullable: true })
  });

  get isOwnerOrCoowner(): boolean {
    return !!this.context.data.owner || !!this.context.data.coowner;
  }

  get accounts(): FormArray {
    return this.form.controls['accounts'] as FormArray;
  }

  get canDelete(): boolean {
    return this.accounts.controls.filter(a => !a.get('deleted')?.value).length > 1;
  }

  get permissions(): FormArray {
    return this.form.controls['permissions'] as FormArray;
  }

  constructor(@Inject(POLYMORPHEUS_CONTEXT) private readonly context: TuiDialogContext<Group | undefined, Group>) {
    this.checkCanDelete();
  }

  getAccount(index: number): FormGroup {
    return this.accounts.controls[index] as FormGroup;
  }

  onAddAccount(): void {
    this.accounts.push(this.acc2form({ id: 0, name: '', fullName: '', currency: '', chain: '', address: '', scale: 2, startBalance: 0, balance: 0 }));
    this.checkCanDelete();
  }

  onRemoveAccount(index: number): void {
    this.accounts.controls[index].get('deleted')?.setValue(true);
    this.checkCanDelete();
  }

  async onEditAccount(index: number) {
    const account = this.getAccount(index);
    const data = await this.#data.editAccount(account.getRawValue());
    if (!!data) {
      account.patchValue(data);
    }
  }

  checkCanDelete() {
    if (!this.canDelete) {
      this.accounts.controls.filter(a => !a.get('deleted')?.value)[0]?.get('name')?.disable();
    } else {
      this.accounts.controls.forEach(a => a.get('name')?.enable());
    }
  }

  getPermission(index: number): FormGroup {
    return this.permissions.controls[index] as FormGroup;
  }

  onSelectedUser(user: string): void {
    this.selected = user;
  }

  onAddPermission() {
    const option = this.rights.value || this.options[0];
    this.permissions.push(this.user2form({ user: { id: 0, email: this.selected, name: '' }, readonly: option.id === 0, admin: option.id === 3 }));
    this.query.setValue('');
    this.rights.setValue(this.options[0]);
  }

  onRemovePermission(index: number): void {
    this.permissions.removeAt(index);
  }

  onToggleAdmin(event: any, index: number): void {
    if (event.target.checked) {
      this.getPermission(index).get('readonly')?.setValue(false);
      this.getPermission(index).get('write')?.setValue(true);
      this.getPermission(index).get('write')?.disable();
    } else {
      this.getPermission(index).get('write')?.enable();
    }
  }

  onToggleWrite(event: any, index: number): void {
    this.getPermission(index).get('readonly')?.setValue(!event.target.checked);
  }

  onCancel() {
    this.context.completeWith(undefined);
  }

  async onSubmit() {
    try {
      let group: Group = this.form.getRawValue();
      group.accounts = group.accounts.map(a => ({ ...a, currency: a.currency.toUpperCase(), scale: scale[a.chain] || 2 }));
      group = await firstValueFrom(this.#api.saveGroup(group));
      this.context.completeWith(group);
    } catch (error) {
      this.#alerts.printError(error);
    }
  }
}
