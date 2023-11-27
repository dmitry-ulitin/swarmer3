import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AccountsComponent } from '../accounts/accounts.component';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, AccountsComponent],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss'
})
export class HomeComponent {

}
