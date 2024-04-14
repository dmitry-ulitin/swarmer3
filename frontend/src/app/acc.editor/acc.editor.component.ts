import { Component, Inject, inject } from '@angular/core';
import { TuiButtonModule, TuiDataListModule, TuiDialogContext, TuiTextfieldControllerModule } from '@taiga-ui/core';
import { Group, Permission } from '../models/group';
import { POLYMORPHEUS_CONTEXT } from '@tinkoff/ng-polymorpheus';
import { DataService } from '../services/data.service';
import { TuiAutoFocusModule, TuiDestroyService, TuiLetModule } from '@taiga-ui/cdk';
import { ApiService } from '../services/api.service';
import { AlertService } from '../services/alert.service';
import { FormArray, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { debounceTime, firstValueFrom, map, of, switchMap, tap } from 'rxjs';
import { Account } from '../models/account';
import { TuiComboBoxModule, TuiDataListWrapperModule, TuiFilterByInputPipeModule, TuiInputModule, TuiInputNumberModule, TuiSelectModule } from '@taiga-ui/kit';
import { AuthService } from '../services/auth.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-acc.editor',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TuiButtonModule, TuiInputModule, TuiInputNumberModule, TuiComboBoxModule, TuiSelectModule,
    TuiTextfieldControllerModule, TuiDataListModule, TuiDataListWrapperModule, TuiFilterByInputPipeModule, TuiLetModule, TuiAutoFocusModule],
  templateUrl: './acc.editor.component.html',
  styleUrl: './acc.editor.component.scss'
})
export class AccEditorComponent {
  #data = inject(DataService);
  #api = inject(ApiService);
  #alerts = inject(AlertService);
  #auth = inject(AuthService);
  currencies = this.#data.currencies();
  options = [{ id: 0, name: 'read' }, { id: 1, name: 'write' }, { id: 3, name: 'admin' }];
  rights = new FormControl(this.options[0]);
  query = new FormControl('');
  selected = '';
  users$ = this.query.valueChanges.pipe(tap(v => this.selected=''),debounceTime(500), switchMap(q => !!q && q.length > 2 ? this.#api.getUsers(q || '') : of([])),
    map(a => a.filter(u => u !== this.form.value.ownerEmail && !this.permissions.controls.find(p => p.value.user.email === u))));

  acc2form = (acc: Account) => new FormGroup({
    id: new FormControl(acc.id, { nonNullable: true }),
    name: new FormControl(acc.name, { nonNullable: true }),
    fullname: new FormControl(acc.fullname, { nonNullable: true }),
    currency: new FormControl(acc.currency || this.#auth.claims().currency || 'EUR', { nonNullable: true, validators: [Validators.required] }),
    start_balance: new FormControl(acc.start_balance, { nonNullable: true, validators: [Validators.required] }),
    balance: new FormControl(acc.balance, { nonNullable: true, validators: [Validators.required] }),
    deleted: new FormControl(acc.deleted, { nonNullable: true })
  });

  user2form = (p: Permission) => new FormGroup({
    user: new FormControl(p.user, { nonNullable: true }),
    is_readonly: new FormControl(p.is_readonly && !p.is_admin, { nonNullable: true }),
    is_write: new FormControl(!p.is_readonly || p.is_admin, { nonNullable: true }),
    is_admin: new FormControl(p.is_admin, { nonNullable: true })
  });

  form = new FormGroup({
    id: new FormControl(this.context.data.id, { nonNullable: true }),
    fullname: new FormControl(this.context.data.fullname, { nonNullable: true, validators: [Validators.required] }),
    is_owner: new FormControl(this.context.data.is_owner, { nonNullable: true }),
    is_coowner: new FormControl(this.context.data.is_coowner, { nonNullable: true }),
    is_shared: new FormControl(this.context.data.is_shared, { nonNullable: true }),
    accounts: new FormArray(this.context.data.accounts.map(this.acc2form)),
    permissions: new FormArray(this.context.data.permissions.map(this.user2form)),
    ownerEmail: new FormControl(this.context.data.ownerEmail, { nonNullable: true })
  });

  get isOwnerOrCoowner(): boolean {
    return !!this.form.value.is_owner || !!this.form.value.is_coowner;
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
    this.accounts.push(this.acc2form({id: 0, name: '', fullname: '', currency: '', start_balance: 0, balance: 0}));
    this.checkCanDelete();
  }

  onRemoveAccount(index: number): void {
    this.accounts.controls[index].get('deleted')?.setValue(true);
    this.checkCanDelete();
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
    this.permissions.push(this.user2form({user: {id:0, email: this.selected, name: ''}, is_readonly: option.id === 0, is_admin: option.id === 3}));
    this.query.setValue('');
    this.rights.setValue(this.options[0]);
  }

  onRemovePermission(index: number): void {
    this.permissions.removeAt(index);
  }

  onToggleAdmin(event: any, index: number): void {
    if (event.target.checked) {
      this.getPermission(index).get('is_readonly')?.setValue(false);
      this.getPermission(index).get('is_write')?.setValue(true);
      this.getPermission(index).get('is_write')?.disable();
    } else {
      this.getPermission(index).get('is_write')?.enable();
    }
  }

  onToggleWrite(event: any, index: number): void {
    this.getPermission(index).get('is_readonly')?.setValue(!event.target.checked);
  }

  onCancel() {
    this.context.completeWith(undefined);
  }

  async onSubmit() {
    try {
      let group: Group = this.form.getRawValue();
      group = await firstValueFrom(this.#api.saveGroup({...group, accounts: group.accounts.map(a => ({...a, currency: a.currency.toUpperCase()}))}));
      this.context.completeWith(group);
    } catch (error) {
      this.#alerts.printError(error);
    }
  }
}
