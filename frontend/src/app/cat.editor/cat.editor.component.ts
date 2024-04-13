import { Component, Inject, computed, inject } from '@angular/core';
import { DataService } from '../services/data.service';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TuiButtonModule, TuiDialogContext, TuiTextfieldControllerModule } from '@taiga-ui/core';
import { TuiDataListWrapperModule, TuiInputModule, TuiSelectModule } from '@taiga-ui/kit';
import { POLYMORPHEUS_CONTEXT } from '@tinkoff/ng-polymorpheus';
import { Category } from '../models/category';
import { TuiFilterPipeModule } from '@taiga-ui/cdk';
import { TransactionType } from '../models/transaction';
import { ApiService } from '../services/api.service';
import { AlertService } from '../services/alert.service';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'app-cat.editor',
  standalone: true,
  imports: [ReactiveFormsModule, TuiButtonModule, TuiInputModule, TuiSelectModule, TuiTextfieldControllerModule, TuiDataListWrapperModule, TuiFilterPipeModule],
  templateUrl: './cat.editor.component.html',
  styleUrl: './cat.editor.component.scss'
})
export class CatEditorComponent {
  #data = inject(DataService);
  #api = inject(ApiService);
  #alerts = inject(AlertService);
  categories = computed(() => this.#data.state().categories);
  form = new FormGroup({
    id: new FormControl(this.context.data.id, { nonNullable: true }),
    name: new FormControl(this.context.data.name, { nonNullable: true, validators: [Validators.required] }),
    fullname: new FormControl(this.context.data.fullname, { nonNullable: true }),
    level: new FormControl(this.context.data.level, { nonNullable: true }),
    parent: new FormControl(this.categories().find((c) => c.id === this.context.data.parent_id), { nonNullable: true, validators: [Validators.required] }),
    parent_id: new FormControl(this.context.data.parent_id, { nonNullable: true }),
    type: new FormControl(this.context.data.type, { nonNullable: true }),
  });

  get categoryParent(): string {
    const parent = this.form.controls['parent'].value;
    return !!parent?.level ? (parent.fullname + ' / ') : '';
  }

  get type(): TransactionType {
    return this.form.controls['type'].value;
  }

  readonly matcher = (category: Category, type: TransactionType): boolean => {
    if (category.type == type) {
      // Check if the category is not a child of the current category 
      let id = this.form.value.id;
      let parent_id = category.id;
      while (!!parent_id) {
        if (parent_id === id) {
          return false;
        }
        const parent = this.categories().find(c => c.id == parent_id);
        parent_id = parent?.parent_id || 0;
      }
      return true;
    }
    return false;
  }


  constructor(@Inject(POLYMORPHEUS_CONTEXT) private readonly context: TuiDialogContext<Category | undefined, Category>) { }

  async onSubmit() {
    try {
      if (this.form.valid) {
        let category = this.form.getRawValue() as Category;
        category.parent_id = this.form.getRawValue().parent?.id || null;
        category = await firstValueFrom(this.#api.saveCategory(category));
        this.context.completeWith(category);
      }
    } catch (error) {
      this.#alerts.printError(error);
    }
  }

  onCancel() {
    this.context.completeWith(undefined);
  }
}
