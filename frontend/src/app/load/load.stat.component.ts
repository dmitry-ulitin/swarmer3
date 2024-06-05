import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, Inject } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { TuiButtonModule, TuiDialogContext } from '@taiga-ui/core';
import { TuiDataListWrapperModule, TuiFileLike, TuiInputFilesModule, TuiSelectModule } from '@taiga-ui/kit';
import { POLYMORPHEUS_CONTEXT } from '@tinkoff/ng-polymorpheus';
import { Subject } from 'rxjs';

@Component({
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, ReactiveFormsModule, TuiButtonModule, TuiInputFilesModule, TuiSelectModule, TuiDataListWrapperModule],
  templateUrl: './load.stat.component.html',
  styleUrl: './load.stat.component.scss'
})
export class LoadStatComponent {
  readonly banks = [{ id: 1, name: 'LHV' }, { id: 2, name: 'Tinkoff' }, { id: 3, name: 'Сбербанк' }, { id: 4, name: 'Альфа-Банк' }];
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
