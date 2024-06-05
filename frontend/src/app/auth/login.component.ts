import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../services/auth.service';
import { TuiButtonModule, TuiLabelModule, TuiTextfieldControllerModule } from '@taiga-ui/core';
import { AlertService } from '../services/alert.service';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TuiInputModule, TuiInputPasswordModule } from '@taiga-ui/kit';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-login',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, TuiButtonModule, TuiInputModule, TuiInputPasswordModule, TuiLabelModule, TuiTextfieldControllerModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {
  #auth = inject(AuthService);
  #alerts = inject(AlertService);
  form = new FormGroup({
    username: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.email] }),
    password: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
  });

  async onLogin() {
    try {
      await this.#auth.login(this.form.value);
    } catch (err) {
      this.#alerts.printError(err);
    }
  }
}
