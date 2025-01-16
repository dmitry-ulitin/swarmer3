import { TuiTextfieldControllerModule, TuiInputModule, TuiSelectModule } from "@taiga-ui/legacy";
import { ChangeDetectionStrategy, Component, Inject, computed, inject } from '@angular/core';
import { DataService } from '../services/data.service';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TuiDialogContext, TuiButton } from '@taiga-ui/core';
import { TuiDataListWrapper } from '@taiga-ui/kit';
import { POLYMORPHEUS_CONTEXT } from '@taiga-ui/polymorpheus';
import { Category } from '../models/category';
import { TuiAutoFocus, TuiFilterPipe } from '@taiga-ui/cdk';
import { TransactionType } from '../models/transaction';
import { ApiService } from '../services/api.service';
import { AlertService } from '../services/alert.service';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'app-cat.editor',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, TuiButton, TuiInputModule, TuiSelectModule, TuiTextfieldControllerModule,
            TuiDataListWrapper, TuiFilterPipe, TuiAutoFocus],
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
    fullName: new FormControl(this.context.data.fullName, { nonNullable: true }),
    level: new FormControl(this.context.data.level, { nonNullable: true }),
    parent: new FormControl(this.categories().find((c) => c.id === this.context.data.parentId), { nonNullable: true, validators: [Validators.required] }),
    parentId: new FormControl(this.context.data.parentId, { nonNullable: true }),
    type: new FormControl(this.context.data.type, { nonNullable: true }),
  });

  get categoryParent(): string {
    const parent = this.form.controls['parent'].value;
    return !!parent?.level ? (parent.fullName + ' / ') : '';
  }

  get type(): TransactionType {
    return this.form.controls['type'].value;
  }

  readonly matcher = (category: Category, type: TransactionType): boolean => {
    if (category.type == type) {
      // Check if the category is not a child of the current category 
      let id = this.form.value.id;
      let parentId = category.id;
      while (!!parentId) {
        if (parentId === id) {
          return false;
        }
        const parent = this.categories().find(c => c.id == parentId);
        parentId = parent?.parentId || 0;
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
        category.parentId = this.form.getRawValue().parent?.id || null;
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
