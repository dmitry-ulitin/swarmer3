import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { HeaderComponent } from './header/header.component';
import { DataService } from './services/data.service';
import { AuthService } from './services/auth.service';
import { TuiRoot } from '@taiga-ui/core';

@Component({
  selector: 'app-root',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, RouterOutlet, HeaderComponent, TuiRoot],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent {
  #auth = inject(AuthService);
  #data = inject(DataService);

  constructor() {
    if (this.#auth.isAuthenticated()) this.#data.init();
  }
}
