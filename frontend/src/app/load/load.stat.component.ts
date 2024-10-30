import { TuiSelectModule } from "@taiga-ui/legacy";
import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, Inject } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { TuiDialogContext, TuiButton } from '@taiga-ui/core';
import { TuiFileLike, TuiDataListWrapper, TuiFiles } from '@taiga-ui/kit';
import { POLYMORPHEUS_CONTEXT } from '@taiga-ui/polymorpheus';
import { Subject } from 'rxjs';

@Component({
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, ReactiveFormsModule, TuiButton, TuiFiles, TuiSelectModule, TuiDataListWrapper],
  templateUrl: './load.stat.component.html',
  styleUrl: './load.stat.component.scss'
})
export class LoadStatComponent {
  readonly banks = [{ id: 1, name: 'LHV', accept: ".csv" }, { id: 2, name: 'Tinkoff', accept: ".csv" }, { id: 3, name: 'Сбербанк', accept: ".pdf" },
    { id: 4, name: 'Альфа-Банк', accept: ".xlsx" }, { id: 5, name: 'Unicredit', accept: ".xls" }];
  readonly files = new FormControl();
  readonly bank = new FormControl(this.banks[0], { nonNullable: true, validators: [Validators.required] });
  readonly rejectedFiles$ = new Subject<TuiFileLike | null>();

  constructor(@Inject(POLYMORPHEUS_CONTEXT) private readonly context: TuiDialogContext<{ bank: number, file: TuiFileLike } | null, undefined>) {
  }

  onReject(file: TuiFileLike | readonly TuiFileLike[]): void {
    this.rejectedFiles$.next(file as TuiFileLike);
  }

  removeFile(): void {
    this.files.setValue(null);
  }

  clearRejected(): void {
    this.rejectedFiles$.next(null);
  }

  onNext() {
    this.context.completeWith({ bank: this.bank.value.id, file: this.files.value });
  }

  onCancel() {
    this.context.completeWith(null);
  }
}
