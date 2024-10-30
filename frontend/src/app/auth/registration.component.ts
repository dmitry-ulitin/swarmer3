import { TuiTextfieldControllerModule, TuiComboBoxModule, TuiInputModule, TuiInputPasswordModule } from "@taiga-ui/legacy";
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { AlertService } from '../services/alert.service';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { TuiButton, TuiLabel } from '@taiga-ui/core';
import { TuiDataListWrapper, TuiFilterByInputPipe } from '@taiga-ui/kit';
import { CURRENCIES_EN } from '../models/currencies';

@Component({
  selector: 'app-registration',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, TuiButton, TuiInputModule, TuiInputPasswordModule,
            TuiLabel, TuiTextfieldControllerModule, TuiComboBoxModule, TuiDataListWrapper, TuiFilterByInputPipe],
  templateUrl: './registration.component.html',
  styleUrl: './registration.component.scss'
})
export class RegistrationComponent {
  #auth = inject(AuthService);
  #alerts = inject(AlertService);
  form = new FormGroup({
    email: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.email] }),
    password: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    name: new FormControl(''),
    currency: new FormControl('EUR', { nonNullable: true, validators: [Validators.required] }),
  });
  currencies = Object.keys(CURRENCIES_EN);


  get defaultName(): string {
    return this.form.value.email?.replace(/@.*/, '') || 'Enter your name';
  }

  async onRegister() {
    try {
      await this.#auth.register({...this.form.value, currency: this.form.value.currency?.toUpperCase(), name: this.form.value.name || this.defaultName});
    } catch (err) {
      this.#alerts.printError(err);
    }
  }
}
