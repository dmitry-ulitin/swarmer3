import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../services/auth.service';
import { TuiButtonModule } from '@taiga-ui/core';
import { AlertService } from '../services/alert.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, TuiButtonModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {
  #auth = inject(AuthService);
  #alerts = inject(AlertService);

  onLogin() {
    try {
      this.#auth.login('dmitry.ulitin@gmail.com', '123456');
    } catch (err) {
      this.#alerts.printError(err);
    }
  }
}
