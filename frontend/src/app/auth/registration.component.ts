import { Component, inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { AlertService } from '../services/alert.service';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { TuiButtonModule, TuiLabelModule, TuiTextfieldControllerModule } from '@taiga-ui/core';
import { TuiInputModule, TuiInputPasswordModule } from '@taiga-ui/kit';

@Component({
  selector: 'app-registration',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, TuiButtonModule, TuiInputModule, TuiInputPasswordModule, TuiLabelModule, TuiTextfieldControllerModule],
  templateUrl: './registration.component.html',
  styleUrl: './registration.component.scss'
})
export class RegistrationComponent {
  #auth = inject(AuthService);
  #alerts = inject(AlertService);
  form = new FormGroup({
    username: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.email] }),
    password: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    name: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    currency: new FormControl('EUR', { nonNullable: true, validators: [Validators.required] }),
  });

  get defaultName(): string {
    return this.form.value.username?.replace(/@.*/, '') || 'Enter your name';
  }

  async onRegister() {
    try {
      await this.#auth.login(this.form.value);
    } catch (err) {
      this.#alerts.printError(err);
    }
  }
}
