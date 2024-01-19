import { Injectable, NgZone, inject } from '@angular/core';
import { TuiAlertService } from '@taiga-ui/core';

@Injectable({
  providedIn: 'root'
})
export class AlertService {
  #alertService = inject(TuiAlertService);
  #zone = inject(NgZone);

  printSuccess(message: string) {
    this.#zone.run(() => this.#alertService.open(message).subscribe());
  }

  printError(error: any) {
    if (error?.status !== 403) {
      const statusText: { [id: string]: string } = { 403: 'Forbidden', 500: 'Internal Server Error' };
      const message = statusText[error?.status] || error?.statusText || error?.message || error;
      this.#zone.run(() => this.#alertService.open(message, { status: 'error' }).subscribe());
    }
  }
}
