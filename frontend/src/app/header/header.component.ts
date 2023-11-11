import { Component, ViewChild, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../services/auth.service';
import { TuiButtonModule, TuiDataListModule, TuiHostedDropdownComponent, TuiHostedDropdownModule, TuiModeModule, TuiSvgModule } from '@taiga-ui/core';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, TuiButtonModule, TuiModeModule, TuiHostedDropdownModule, TuiDataListModule, TuiSvgModule],
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss'
})
export class HeaderComponent {
  auth = inject(AuthService);
  @ViewChild('userMenu') userMenu?: TuiHostedDropdownComponent;

  onLogout() {
    this.userMenu?.close();
    this.auth.logout();
  }
}
