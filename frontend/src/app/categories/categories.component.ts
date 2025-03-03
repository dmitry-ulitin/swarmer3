import { TuiButton } from "@taiga-ui/core";
import { TuiTree } from "@taiga-ui/kit";
import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { DataService } from '../services/data.service';
import { Category } from '../models/category';
import { EMPTY_ARRAY, TuiHandler } from '@taiga-ui/cdk';
import { TransactionType } from '../models/transaction';

interface TreeNode {
  readonly category: Category;
  readonly children: TreeNode[];
}

@Component({
  selector: 'app-categories',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TuiButton, TuiTree],
  templateUrl: './categories.component.html',
  styleUrl: './categories.component.scss'
})
export class CategoriesComponent {
  #data = inject(DataService);
  categories = computed(() => {
    const tree: TreeNode[] = [];
    map2tree(this.#data.state().categories, 0, tree, this.map)
    return tree;
  });
  readonly handler: TuiHandler<TreeNode, readonly TreeNode[]> = item => item.children || EMPTY_ARRAY;
  readonly map = new Map<TreeNode, boolean>();
  selected: number = TransactionType.Expense;

  get isEditable(): boolean {
    // check if map contains all parents of the selected node
    let parentId = this.selected;
    while (parentId > TransactionType.Correction) {
      let key = [...this.map.keys()].find(n => n.category.id === parentId);
      if (!key || parentId !== this.selected && key.children.length > 0 && !this.map.get(key)) {
        return false;
      }
      parentId = key.category.parentId || 1;
    };
    return this.selected > TransactionType.Correction;
  }

  onRefresh() {
    this.#data.getCategories();
  }

  async onAdd() {
    const category = await this.#data.createCategory(this.selected);
    // expand selected node
    if (category) {
      let key = [...this.map.keys()].find(n => n.category.id === this.selected);
      if (key) {
        this.map.set(key, true);
        this.selected = category.id;
      }
    }
  }

  async onEdit() {
    await this.#data.editCategory(this.selected);
  }

  async onDelete() {
    if (await this.#data.deleteCategory(this.selected)) {
      // delete selected node
      let key = [...this.map.keys()].find(n => n.category.id === this.selected);
      if (key) {
        this.map.delete(key);
        this.selected = key.category.parentId || TransactionType.Expense;
      }
    }
  }

  setAsSelected(node: TreeNode) {
    this.selected = node.category.id;
  }
}

function map2tree(data: Category[], index: number, tree: TreeNode[], map: Map<TreeNode, boolean>) {
  const level = data[index].level;
  while (index < data.length && data[index].level >= level) {
    if (data[index].type === TransactionType.Correction) {
      index++;
      continue;
    }
    if (data[index].level > level) {
      index = map2tree(data, index, tree[tree.length - 1].children, map);
    } else {
      const item = { category: data[index++], children: []};
      tree.push(item);
      
      const key = [...map.keys()].find(n => n.category.id === item.category.id);
      if (key) {
        map.set(item,  map.get(key) || false);
        map.delete(key);
      } else {
        map.set(item, false);
      }
    }
  }
  return index;
}
