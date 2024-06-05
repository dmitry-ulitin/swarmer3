import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, Inject } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { TuiButtonModule, TuiDialogContext } from '@taiga-ui/core';
import { TuiFileLike, TuiInputFilesModule } from '@taiga-ui/kit';
import { POLYMORPHEUS_CONTEXT } from '@tinkoff/ng-polymorpheus';
import { Subject } from 'rxjs';

@Component({
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, ReactiveFormsModule, TuiButtonModule, TuiInputFilesModule],
  templateUrl: './load.dump.component.html',
  styleUrl: './load.dump.component.scss'
})
export class LoadDumpComponent {
  readonly files = new FormControl();
  readonly rejectedFiles$ = new Subject<TuiFileLike | null>();

  constructor(@Inject(POLYMORPHEUS_CONTEXT) private readonly context: TuiDialogContext<TuiFileLike | null, undefined>) {
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
    this.context.completeWith(this.files.value);
  }

  onCancel() {
    this.context.completeWith(null);
  }
}
