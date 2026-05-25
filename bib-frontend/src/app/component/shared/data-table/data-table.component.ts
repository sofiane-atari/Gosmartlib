import {
  Component,
  ContentChildren,
  input,
  output,
  QueryList,
} from '@angular/core';
import { CommonModule } from '@angular/common';

import { DataTableColumn } from './data-table.types';
import { CellTemplateDirective } from './cell-template.directive';
import { PagingComponent, PagingMeta } from '../paging/paging.component';

@Component({
  selector: 'app-data-table',
  standalone: true,
  imports: [CommonModule, CellTemplateDirective, PagingComponent],
  templateUrl: './data-table.component.html',
  styleUrls: ['./data-table.component.css'],
})
export class DataTableComponent<T> {
  data = input<T[]>([]);
  columns = input<DataTableColumn[]>([]);
  trackBy = input<((item: T) => string | number) | undefined>(undefined);
  loading = input(false);
  skeletonRows = input(5);
  emptyMessage = input('Geen items gevonden.');

  pagingMeta = input<PagingMeta | null>(null);
  pageChange = output<number>();

  @ContentChildren(CellTemplateDirective)
  private readonly templateDirectives!: QueryList<CellTemplateDirective>;

  protected getTemplateRef(columnId: string) {
    const dir = this.templateDirectives?.find(
      (d) => d.cellTemplate() === columnId,
    );
    return dir?.templateRef ?? null;
  }
}
