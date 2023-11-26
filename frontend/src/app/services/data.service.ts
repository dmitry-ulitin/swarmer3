import { Injectable, computed, inject, signal } from '@angular/core';
import { ApiService } from './api.service';
import { Group } from '../models/group';
import { firstValueFrom } from 'rxjs';
import { Category } from '../models/category';
import { AlertService } from './alert.service';
import { total } from '../models/balance';

export interface DataState {
  // groups
  groups: Group[];
  expanded: number[];
  accounts: number[];
  // categories
  categories: Category[];
}

@Injectable({
  providedIn: 'root'
})
export class DataService {
  #api = inject(ApiService);
  #default = { groups: [], expanded: [], accounts: [], categories: [] }
  #state = signal<DataState>(this.#default);
  #alerts = inject(AlertService);
  groups = computed(() => this.#state().groups);
  expanded = computed(() => this.#state().expanded);

  async init() {
    await Promise.all([this.getGroups(), this.getCategories()]);
  }

  reset() {
    this.#state.set({ ...this.#default });
  }

  async getGroups() {
    try {
      const groups = await firstValueFrom(this.#api.getGroups(''));
      this.#state.update(state => ({ ...state, groups }));
    } catch (err) {
      this.#alerts.printError(err);
    }
  }

  async getCategories() {
    try {
      const categories = await firstValueFrom(this.#api.getCategories());
      this.#state.update(state => ({ ...state, categories }));
    } catch (err) {
      this.#alerts.printError(err);
    }
  }
}
