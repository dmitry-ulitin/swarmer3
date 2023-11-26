import { Injectable, computed, effect, inject, signal } from '@angular/core';
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

  #tokenKey = 'app.token';
  #state = signal<string | null>(localStorage.getItem(this.#tokenKey));
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

  constructor() {
    effect(() => { if (this.isAuthenticated()) this.#data.init(); else this.#data.reset(); })
  }

  async login(username: string, password: string) {
    const response = await firstValueFrom(this.#api.login(username, password));
    if (!response?.access_token) {
      throw new Error('Incorrect username or password');
    }
    localStorage.setItem(this.#tokenKey, response.access_token);
    this.#state.set(response.access_token);
    this.#router.navigate(['']);
  }

  logout() {
    if (this.isAuthenticated()) {
      localStorage.removeItem(this.#tokenKey);
      this.#state.set(null);
    }
    window.location.reload();
  }
}
