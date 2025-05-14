import { Injectable, computed, inject, signal } from '@angular/core';
import { ApiService } from './api.service';
import { firstValueFrom } from 'rxjs';
import { Router } from '@angular/router';
import { DataService } from './data.service';
import { AlertService } from './alert.service';

export interface Credentials {
  email?: string;
  password?: string;
}

export interface Registration extends Credentials {
  name?: string;
  currency?: string;
}

export interface ChangePassword {
  oldPassword: string;
  newPassword: string;
}


@Injectable({
  providedIn: 'root'
})
export class AuthService {
  #router = inject(Router);
  #api = inject(ApiService);
  #data = inject(DataService);
  #alerts = inject(AlertService);

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

  async login(credentials: Credentials) {
    const response = await firstValueFrom(this.#api.login(credentials));
    if (!response?.token) {
      throw new Error('Incorrect username or password');
    }
    localStorage.setItem(this.#tokenKey, response.token);
    this.#state.set(response.token);
    this.#router.navigate(['']);
    this.#data.init();
  }

  async register(registration: Registration) {
    const response = await firstValueFrom(this.#api.register(registration));
    if (!response?.token) {
      throw new Error('Registration error');
    }
    localStorage.setItem(this.#tokenKey, response.token);
    this.#state.set(response.token);
    this.#router.navigate(['']);
    this.#data.init();
  }

  async changePassword(changePassword: ChangePassword) {
    await firstValueFrom(this.#api.changePassword(changePassword));
    this.#alerts.printSuccess('Password changed successfully');
    this.#router.navigate(['']);
  }

  logout() {
    if (this.isAuthenticated()) {
      localStorage.removeItem(this.#tokenKey);
      this.#state.set(null);
      this.#data.reset();
    }
    window.location.reload();
  }
}
