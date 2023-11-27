import { Component, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DataService } from '../services/data.service';
import { Group, total } from '../models/group';
import { Account } from '../models/account';
import { TuiLinkModule, TuiSvgModule } from '@taiga-ui/core';

@Component({
  selector: 'app-accounts',
  standalone: true,
  imports: [CommonModule, TuiSvgModule, TuiLinkModule],
  templateUrl: './accounts.component.html',
  styleUrl: './accounts.component.scss'
})
export class AccountsComponent {
  data = inject(DataService);
  accounts = computed(() => this.data.state().accounts);


  toggle(group: Group): void {
    this.data.toggleGropup(group.id);
  }

  total(g: Group) {
    return total(g);
  }

  isGroupSelected(group: Group): boolean {
    return !group.accounts.some(a => !a.deleted && !this.isAccountSelected(a));
  }

  isAccountSelected(a: Account): boolean {
    return this.accounts().includes(a.id);
  }

  isGroupExpandable(group: Group): boolean {
    return group.accounts.filter(a => !a.deleted).length > 1;
  }

  selectGroup(group: Group, event: MouseEvent): void {
    event.stopPropagation();
    let selected = group.accounts.map(a => a.id);
    if (event.ctrlKey) {
      if (this.isGroupSelected(group)) {
        selected = this.accounts().filter(a => !selected.includes(a));
      } else {
        selected = [...this.accounts(), ...selected];
      }
    }
    this.data.selectAccounts(selected);
  }

  selectAccount(account: Account, event: MouseEvent): void {
    event.stopPropagation();
    const selected = event.ctrlKey ? (this.isAccountSelected(account) ? this.accounts().filter(a => a !== account.id) : [...this.accounts(), account.id]) : [account.id];
    this.data.selectAccounts(selected);
  }

  newGroup(): void {
    //    this.store.dispatch(new CreateGroup());
  }
}
