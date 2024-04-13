import { Component, computed, inject } from '@angular/core';
import { DataService } from '../services/data.service';
import { Category } from '../models/category';
import { TuiButtonModule } from '@taiga-ui/core';
import { TuiTreeModule } from '@taiga-ui/kit';
import { EMPTY_ARRAY, TuiHandler } from '@taiga-ui/cdk';
import { TransactionType } from '../models/transaction';

interface TreeNode {
  readonly category: Category;
  readonly children: TreeNode[];
}

@Component({
  selector: 'app-categories',
  standalone: true,
  imports: [TuiButtonModule, TuiTreeModule],
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
    let parent_id = this.selected;
    while (parent_id > TransactionType.Correction) {
      let key = [...this.map.keys()].find(n => n.category.id === parent_id);
      if (!key || parent_id !== this.selected && key.children.length > 0 && !this.map.get(key)) {
        return false;
      }
      parent_id = key.category.parent_id || 1;
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
        this.selected = key.category.parent_id || TransactionType.Expense;
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
    if (data[index].level === 0 && data[index].type === TransactionType.Correction) {
      index++;
      continue;
    }
    if (data[index].level > level) {
      index = map2tree(data, index, tree[tree.length - 1].children, map);
    } else {
      const item = { category: data[index++], children: [] };
      tree.push(item);
      const key = [...map.keys()].find(n => n.category.id === item.category.id);
      if (key && map.get(key)) {
        map.delete(key);
        map.set(item, true);
      } else {
        map.set(item, false);
      }
    }
  }
  return index;
}
