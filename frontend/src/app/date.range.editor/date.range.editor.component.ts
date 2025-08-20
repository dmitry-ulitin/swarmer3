import { ChangeDetectionStrategy, Component, Inject, inject } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { TuiDialogContext, TuiButton, TuiTextfield, TuiTextfieldDropdownDirective } from '@taiga-ui/core';
import { POLYMORPHEUS_CONTEXT } from '@taiga-ui/polymorpheus';
import { DateRange, RangeType } from '../models/date.range';
import { TuiInputDateRange } from '@taiga-ui/kit';
import { TuiDayRange } from '@taiga-ui/cdk';

@Component({
  selector: 'app-date-range-editor',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    TuiButton,
    TuiInputDateRange,
    TuiTextfield,
    TuiTextfieldDropdownDirective
  ],
  templateUrl: './date.range.editor.component.html',
  styleUrl: './date.range.editor.component.scss'
})
export class DateRangeEditorComponent {
  form = new FormGroup({
    dateRange: new FormControl(this.context.data, { nonNullable: true })
  });

  constructor(@Inject(POLYMORPHEUS_CONTEXT) private readonly context: TuiDialogContext<DateRange | undefined, TuiDayRange>) {
  }

  onSubmit(): void {
    if (this.form.valid) {
      const range = this.form.getRawValue().dateRange;
      this.context.completeWith(new DateRange(range.toString(), range.from, range.to, RangeType.Custom));
    }
  }

  onCancel(): void {
    this.context.completeWith(undefined);
  }
}