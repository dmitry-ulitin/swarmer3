import { Injectable, computed, inject, signal } from '@angular/core';
import { ApiService } from './api.service';
import { firstValueFrom } from 'rxjs';
import { Router } from '@angular/router';
import { DataService } from './data.service';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  #router = inject(Router);
  #api = inject(ApiService);
  #data = inject(DataService);

  #state = signal<string | null>(null);
  token = this.#state.asReadonly();
  isAuthenticated = computed(() => !!this.#state());
  claims = computed(() => {
    const token = this.#state();
    if (!token) {
      return undefined;
    }
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    return JSON.parse(window.atob(base64));
  });

  async login(username: string, password: string) {
    const response = await firstValueFrom(this.#api.login(username, password));
    if (!response?.access_token) {
      throw new Error('Incorrect username or password');
    }
    this.#state.update(state => response.access_token);
    this.#data.init();
    this.#router.navigate(['']);
  }

  logout() {
    if (this.isAuthenticated()) {
      this.#state.update(state => null);
    }
    this.#data.reset();
    window.location.reload();
  }
}
