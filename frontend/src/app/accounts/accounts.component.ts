import { Component, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DataService } from '../services/data.service';
import { Group } from '../models/group';
import { total } from '../models/balance';
import { Account } from '../models/account';

@Component({
  selector: 'app-accounts',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './accounts.component.html',
  styleUrl: './accounts.component.scss'
})
export class AccountsComponent {
  data = inject(DataService);
  groups = computed(() => this.data.groups().filter(g => !g.deleted));

  toggle(group: Group): void {
//    this.store.dispatch(new ToggleGropup(group.id));
  }

  total(g: Group) {
    return [...total(g).entries()].map(e => ({ value: e[1], currency: e[0] }));
  }

  isAccountSelected(a: Account): boolean {
    return this.data.accounts().includes(a.id);
  }

  isGroupSelected(group: Group): boolean {
    return !group.accounts.some(a => !a.deleted && !this.isAccountSelected(a));
  }

  isGroupExpandable(group: Group): boolean {
    return group.accounts.filter(a => !a.deleted).length > 1;
  }

  selectGroup(group: Group, event: MouseEvent): void {
    event.stopPropagation();
    let selected = group.accounts.map(a => a.id);
    if (event.ctrlKey) {
      if (this.isGroupSelected(group)) {
        selected = this.data.accounts().filter(a => !selected.includes(a));
      } else {
        selected = [...this.data.accounts(), ...selected];
      }
    }
//    this.store.dispatch(new SelectAccounts(selected));
  }

  selectAccount(account: Account, event: MouseEvent): void {
    event.stopPropagation();
    const accounts = event.ctrlKey ? (this.isAccountSelected(account) ? this.data.accounts().filter(a => a !== account.id) : [...this.data.accounts(), account.id]) : [account.id];
//    this.store.dispatch(new SelectAccounts(accounts));
  }

  newGroup(): void {
//    this.store.dispatch(new CreateGroup());
  }
}
