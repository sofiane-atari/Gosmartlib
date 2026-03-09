import { Component, computed, inject, signal, DestroyRef } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { LabelService } from '../../services/label/label.service';
import { StatCardComponent } from '../stat-card/stat-card.component';
import { LabelsListComponent } from '../labels-list/labels-list.component';
import { toSignal } from '@angular/core/rxjs-interop';
import { CreateLabelComponent } from '../create-label/create-label.component';
import { SearchBarComponent } from '../search-bar/search-bar.component';
import {
  PageingOpt,
  PaginatedResponse,
} from '../../model/paginated-response.model.ts';
import { ActivatedRoute, Router } from '@angular/router';
import { PagingComponent } from '../paging/paging.component';
import { switchMap } from 'rxjs/operators';

type PopupState = 'create' | 'edit' | null;

@Component({
  selector: 'app-dashboard-labels',
  standalone: true,
  imports: [
    FormsModule,
    StatCardComponent,
    LabelsListComponent,
    CreateLabelComponent,
    SearchBarComponent,
    PagingComponent,
  ],
  templateUrl: './dashboard-labels.component.html',
  styleUrls: ['./dashboard-labels.component.css'],
})
export class DashboardLabelsComponent {
  private labelService = inject(LabelService);
  private destroyRef = inject(DestroyRef);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  protected success = signal<string | null>(null);
  protected error = signal<string | null>(null);
  protected popUp = signal<PopupState>(null);

  public searchQuery = signal('');

  // Reactive observable that updates when queryParams change
  private labels$ = this.route.queryParamMap.pipe(
    switchMap((queryParams) => {
      const page = Number(queryParams.get('page')) || 0;
      const pageSize = Number(queryParams.get('pageSize')) || 20;

      return this.labelService.getLabelsByPage({
        labelGetOpt: { includeBookCount: true },
        pagingOpt: { page, pageSize },
      });
    }),
  );

  response = toSignal(this.labels$, {
    initialValue: {
      data: [],
      meta: { currentPage: 0, pageSize: 20, totalItems: 0, totalPages: 0 },
    } as PaginatedResponse<never>,
  });

  labels = computed(() => this.response()?.data ?? []);

  pagingMeta = computed(() => this.response()?.meta);

  totalCount = computed(() => this.pagingMeta()?.totalItems ?? 0);

  activeCount = computed(
    () => this.labels().filter((l) => l.booksCount! > 0).length,
  );

  unusedCount = computed(
    () => this.labels().filter((l) => l.booksCount! === 0).length,
  );

  filteredLabels = computed(() =>
    this.labels().filter(
      (l) =>
        l.description
          .toLowerCase()
          .includes(this.searchQuery().toLowerCase()) ||
        l.name.toLowerCase().includes(this.searchQuery().toLowerCase()),
    ),
  );

  onSearch(query: string) {
    this.searchQuery.set(query);
  }

  onPageChange(page: number): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { page },
      queryParamsHandling: 'merge',
    });
  }

  setPopup(popup: PopupState) {
    this.popUp.set(popup);
  }

  closePopup() {
    this.setPopup(null);
  }

  onSuccess(message: string) {
    this.success.set(message);
    this.closePopup();

    const timeoutId = setTimeout(() => {
      this.success.set(null);
    }, 5000);

    this.destroyRef.onDestroy(() => clearTimeout(timeoutId));
  }

  onError(message: string) {
    this.error.set(message);
  }
}
