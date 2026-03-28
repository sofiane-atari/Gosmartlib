import { Component, input, output } from '@angular/core';

export interface PagingMeta {
  currentPage: number;
  pageSize: number;
  totalItems: number;
  totalPages: number;
}

@Component({
  selector: 'app-paging',
  standalone: true,
  imports: [],
  templateUrl: './paging.component.html',
  styleUrls: ['./paging.component.css'],
})
export class PagingComponent {
  meta = input.required<PagingMeta>();
  pageChange = output<number>();

  get lastPage(): number {
    return this.meta().totalPages - 1;
  }

  get pages(): number[] {
    const meta = this.meta();
    const pages: number[] = [];
    const start = Math.max(0, meta.currentPage - 2);
    const end = Math.min(meta.totalPages - 1, meta.currentPage + 2);

    for (let i = start; i <= end; i++) {
      pages.push(i);
    }
    return pages;
  }

  get canGoPrevious(): boolean {
    return this.meta().currentPage > 0;
  }

  get canGoNext(): boolean {
    return this.meta().currentPage < this.meta().totalPages - 1;
  }

  goToPage(page: number): void {
    if (page >= 0 && page <= this.meta().totalPages - 1) {
      this.pageChange.emit(page);
    }
  }

  previousPage(): void {
    this.goToPage(this.meta().currentPage - 1);
  }

  nextPage(): void {
    this.goToPage(this.meta().currentPage + 1);
  }
}