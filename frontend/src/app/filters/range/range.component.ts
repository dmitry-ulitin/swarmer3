import { ChangeDetectionStrategy, Component, ViewChild, inject } from '@angular/core';
import { DataService } from '../../services/data.service';
import { TuiDataList, TuiDropdown, TuiDropdownOpen, TuiIcon, TuiButton } from '@taiga-ui/core';
import { DateRange } from '../../models/date.range';
import { TuiChevron } from '@taiga-ui/kit';

@Component({
  selector: 'app-range',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TuiButton, TuiDropdown, TuiDataList, TuiIcon, TuiChevron],
  templateUrl: './range.component.html',
  styleUrl: './range.component.scss'
})
export class RangeComponent {
  data = inject(DataService);
  @ViewChild(TuiDropdownOpen) component?: TuiDropdownOpen;
  options = [DateRange.all(), DateRange.last30(), DateRange.last90(), DateRange.lastYear(), DateRange.month(), DateRange.year()];
  open = false;

  onClick(option: DateRange) {
    this.open = false;
    this.data.setRange(option);
  }

  onCustomRange() {
    this.data.setCustomRange();
  }

  prev(option: DateRange) {
    this.data.setRange(option.prev());
  }

  next(option: DateRange) {
    this.data.setRange(option.next());
  }
}
