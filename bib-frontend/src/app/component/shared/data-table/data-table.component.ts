import {
  Component,
  ContentChildren,
  HostListener,
  input,
  OnInit,
  output,
  QueryList,
  signal,
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
export class DataTableComponent<T> implements OnInit {
  data = input<T[]>([]);
  columns = input<DataTableColumn[]>([]);
  trackBy = input<((item: T) => string | number) | undefined>(undefined);
  loading = input(false);
  skeletonRows = input(5);
  emptyMessage = input('Geen items gevonden.');

  pagingMeta = input<PagingMeta | null>(null);
  pageChange = output<number>();

  breakpointPx = input(1240);

  @ContentChildren(CellTemplateDirective)
  private readonly templateDirectives!: QueryList<CellTemplateDirective>;

  protected readonly isCompact = signal(false);

  public ngOnInit(): void {
    this.checkBreakpoint();
  }

  @HostListener('window:resize')
  protected onResize(): void {
    this.checkBreakpoint();
  }

  private checkBreakpoint(): void {
    this.isCompact.set(window.innerWidth <= this.breakpointPx());
  }

  protected getTemplateRef(columnId: string) {
    const dir = this.templateDirectives?.find(
      (d) => d.cellTemplate() === columnId,
    );
    return dir?.templateRef ?? null;
  }
}
