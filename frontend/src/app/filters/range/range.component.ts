import { ChangeDetectionStrategy, Component, ViewChild, inject } from '@angular/core';
import { DataService } from '../../services/data.service';
import { TuiButtonModule, TuiDataListModule, TuiHostedDropdownComponent, TuiHostedDropdownModule, TuiSvgModule } from '@taiga-ui/core';
import { DateRange } from '../../models/date.range';

@Component({
  selector: 'app-range',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TuiButtonModule, TuiHostedDropdownModule, TuiDataListModule, TuiSvgModule],
  templateUrl: './range.component.html',
  styleUrl: './range.component.scss'
})
export class RangeComponent {
  data = inject(DataService);
  @ViewChild(TuiHostedDropdownComponent) component?: TuiHostedDropdownComponent;
  options = [DateRange.all(), DateRange.last30(), DateRange.last90(), DateRange.lastYear(), DateRange.month(), DateRange.year()];
  open = false;

  onClick(option: DateRange) {
    this.open = false;
    this.component?.nativeFocusableElement?.focus();
    this.data.setRange(option);
  }

  prev(option: DateRange) {
    this.data.setRange(option.prev());
  }

  next(option: DateRange) {
    this.data.setRange(option.next());
  }
}
