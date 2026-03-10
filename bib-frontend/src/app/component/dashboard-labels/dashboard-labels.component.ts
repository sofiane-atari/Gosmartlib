import { Component, computed, inject, signal, DestroyRef } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { LabelService } from '../../services/label/label.service';
import { StatCardComponent } from '../stat-card/stat-card.component';
import { LabelsListComponent } from '../labels-list/labels-list.component';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { CreateLabelComponent } from '../create-label/create-label.component';
import { SearchBarComponent } from '../search-bar/search-bar.component';
import {
  PageingOpt,
  PaginatedResponse,
} from '../../model/paginated-response.model.ts';
import { ActivatedRoute, Router } from '@angular/router';
import { PagingComponent } from '../paging/paging.component';
import { switchMap, debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { Subject } from 'rxjs';

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
  private refreshTrigger = signal(false);

  // Subject for debounced search
  private searchSubject = new Subject<string>();

  protected success = signal<string | null>(null);
  protected error = signal<string | null>(null);
  protected popUp = signal<PopupState>(null);

  // BookCount filter (0 = any, 1+ = has books, -1 = no books)
  protected bookCountFilter = signal<string>('');

  constructor() {
    // Set up debounced search - waits 300ms after user stops typing
    this.searchSubject
      .pipe(debounceTime(300), distinctUntilChanged())
      .subscribe((searchQuery: string) => {
        this.updateSearchQueryParam(searchQuery);
      });
  }

  private updateSearchQueryParam(query: string): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { search: query || null, page: 0 },
      queryParamsHandling: 'merge',
    });
  }

  // Reactive observable that updates when queryParams change
  private labels$ = this.route.queryParamMap.pipe(
    switchMap((queryParams) => {
      const page = Number(queryParams.get('page')) || 0;
      const pageSize = Number(queryParams.get('pageSize')) || 10;
      const search = queryParams.get('search') || undefined;
      const bookCount = queryParams.get('bookCount') || undefined;

      return this.labelService.getLabelsByPage({
        labelGetOpt: {
          includeBookCount: true,
          search,
          bookCount,
        },
        pagingOpt: { page, pageSize },
      });
    }),
  );
  private response = toSignal(
    toObservable(this.refreshTrigger).pipe(switchMap(() => this.labels$)),
    {
      initialValue: {
        data: [],
        meta: { currentPage: 0, pageSize: 20, totalItems: 0, totalPages: 0 },
      } as PaginatedResponse<never>,
    },
  );
  protected stats = toSignal(
    toObservable(this.refreshTrigger).pipe(
      switchMap(() => this.labelService.getLabelStats()),
    ),
  );
  protected totalActive = computed(
    () => (this.stats()?.totalItems ?? 0) - (this.stats()?.unActiveLabels ?? 0),
  );

  protected labels = computed(() => this.response()?.data ?? []);
  protected pagingMeta = computed(() => this.response()?.meta);

  onSearch(query: string): void {
    // Push to subject for debouncing
    this.searchSubject.next(query);
  }

  onBookCountFilterChange(bookCount: string): void {
    this.bookCountFilter.set(bookCount);
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { bookCount: bookCount || null, page: 0 },
      queryParamsHandling: 'merge',
    });
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

    this.refreshTrigger.set(true);
    this.refreshTrigger.set(false);

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
