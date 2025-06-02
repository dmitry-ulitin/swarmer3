import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { AuthService } from '../services/auth.service';
import { TuiButton, TuiIcon, TuiLabel, TuiTextfield } from '@taiga-ui/core';
import { AlertService } from '../services/alert.service';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { TuiPassword } from '@taiga-ui/kit';

@Component({
    selector: 'app-change-password',
    standalone: true,
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [ReactiveFormsModule, RouterLink, TuiButton, TuiTextfield, TuiIcon, TuiLabel, TuiPassword],
    templateUrl: './change-password.component.html',
    styleUrl: './change-password.component.scss'
})
export class ChangePasswordComponent {
    #auth = inject(AuthService);
    #alerts = inject(AlertService);
    form = new FormGroup({
        oldPassword: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
        newPassword: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
        confirmPassword: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    });

    async onChangePassword() {
        if (this.form.value.newPassword !== this.form.value.confirmPassword) {
            this.#alerts.printError('New passwords do not match');
            return;
        }

        try {
            await this.#auth.changePassword({
                oldPassword: this.form.value.oldPassword!,
                newPassword: this.form.value.newPassword!,
            });
        } catch (err) {
            this.#alerts.printError(err);
        }
    }
}
