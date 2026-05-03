import {
  Component,
  computed,
  inject,
  signal,
  DestroyRef,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap } from 'rxjs/operators';

import { TagService } from '../../../services/tag/tag.service';
import { Tag } from '../../../model/tag/tag.model';
import { PaginatedResponse } from '../../../model/shared/paging/PaginatedResponse.model';
import { HttpErrorResponseError } from '../../../error-reponse.model';

import { PagingComponent } from '../../shared/paging/paging.component';
import { SearchBarComponent } from '../../shared/search-bar/search-bar.component';

type PopupState = 'create' | 'edit' | null;

interface ErrorItem {
  field: string;
  message: string;
}

@Component({
  selector: 'app-tag-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    PagingComponent,
    SearchBarComponent,
  ],
  templateUrl: './tag-dashboard.component.html',
  styleUrls: ['./tag-dashboard.component.css'],
})
export class TagDashboardComponent {
  private tagService = inject(TagService);
  private destroyRef = inject(DestroyRef);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  private refreshTrigger = signal(false);
  private searchSubject = new Subject<string>();

  protected success = signal<string | null>(null);
  protected errors = signal<ErrorItem[]>([]);
  protected popUp = signal<PopupState>(null);

  protected editingTag = signal<Tag | null>(null);

  protected bookCountFilter = signal<string>('');

  protected createName = signal<string>('');
  protected createDescription = signal<string>('');

  protected editName = signal<string>('');
  protected editDescription = signal<string>('');

  constructor() {
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

  private tags$ = this.route.queryParamMap.pipe(
    switchMap((queryParams) => {
      const page = Number(queryParams.get('page')) || 0;
      const pageSize = Number(queryParams.get('pageSize')) || 10;
      const search = queryParams.get('search') || undefined;
      const bookCount = queryParams.get('bookCount') || undefined;

      return this.tagService.getTagsByPage({
        tagGetOpt: {
          includeBookCount: true,
          search,
          bookCount,
        },
        pagingOpt: { page, pageSize },
      });
    }),
  );

  private response = toSignal(
    toObservable(this.refreshTrigger).pipe(switchMap(() => this.tags$)),
    {
      initialValue: {
        data: [],
        meta: {
          currentPage: 0,
          pageSize: 10,
          totalItems: 0,
          totalPages: 0,
        },
      } as PaginatedResponse<Tag>,
    },
  );

  protected stats = toSignal(
    toObservable(this.refreshTrigger).pipe(
      switchMap(() => this.tagService.getTagStats()),
    ),
    {
      initialValue: {
        totalItems: 0,
        unActiveTags: 0,
      },
    },
  );

  protected tags = computed(() => this.response()?.data ?? []);
  protected pagingMeta = computed(() => this.response()?.meta);

  protected totalActive = computed(
    () => (this.stats()?.totalItems ?? 0) - (this.stats()?.unActiveTags ?? 0),
  );

  protected onSearch(query: string): void {
    this.searchSubject.next(query);
  }

  protected onBookCountFilterChange(bookCount: string): void {
    this.bookCountFilter.set(bookCount);

    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { bookCount: bookCount || null, page: 0 },
      queryParamsHandling: 'merge',
    });
  }

  protected onPageChange(page: number): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { page },
      queryParamsHandling: 'merge',
    });
  }

  protected setPopup(popup: PopupState): void {
    this.popUp.set(popup);
  }

  protected openCreatePopup(): void {
    this.createName.set('');
    this.createDescription.set('');
    this.setPopup('create');
  }

  protected openEditPopup(tag: Tag): void {
    this.editingTag.set(tag);
    this.editName.set(tag.name ?? '');
    this.editDescription.set(tag.description ?? '');
    this.setPopup('edit');
  }

  protected closePopup(): void {
    this.setPopup(null);
    this.editingTag.set(null);
  }

  protected createTag(): void {
    const name = this.createName().trim();
    const description = this.createDescription().trim();

    if (!name) {
      this.onError('Naam is verplicht.');
      return;
    }

    this.tagService
      .createTag({
        name,
        description: description || undefined,
      })
      .subscribe({
        next: () => {
          this.onSuccess('Tag succesvol aangemaakt.');
        },
        error: (error: HttpErrorResponseError) => {
          this.onError(error);
        },
      });
  }

  protected updateTag(): void {
    const tag = this.editingTag();

    if (!tag) {
      this.onError('Geen tag geselecteerd om te bewerken.');
      return;
    }

    const name = this.editName().trim();
    const description = this.editDescription().trim();

    if (!name) {
      this.onError('Naam is verplicht.');
      return;
    }

    this.tagService
      .updateTag(tag.id, {
        name,
        description: description || undefined,
      })
      .subscribe({
        next: () => {
          this.onSuccess('Tag succesvol bijgewerkt.');
        },
        error: (error: HttpErrorResponseError) => {
          this.onError(error);
        },
      });
  }

  protected deleteTag(tag: Tag): void {
    const confirmed = confirm(`Ben je zeker dat je "${tag.name}" wilt verwijderen?`);

    if (!confirmed) {
      return;
    }

    this.tagService.deleteTag(tag.id).subscribe({
      next: () => {
        this.onSuccess('Tag succesvol verwijderd.');
      },
      error: (error: HttpErrorResponseError) => {
        this.onError(error);
      },
    });
  }

  protected onSuccess(message: string): void {
    this.success.set(message);

    this.refreshTrigger.set(true);
    this.refreshTrigger.set(false);

    this.closePopup();

    const timeoutId = setTimeout(() => {
      this.success.set(null);
    }, 5000);

    this.destroyRef.onDestroy(() => clearTimeout(timeoutId));
  }

  protected onError(error: string | HttpErrorResponseError): void {
    this.refreshTrigger.set(true);
    this.refreshTrigger.set(false);

    if (typeof error === 'string') {
      this.errors.set([{ field: 'error', message: error }]);

      const timeoutId = setTimeout(() => {
        this.errors.set([]);
      }, 5000);

      this.destroyRef.onDestroy(() => clearTimeout(timeoutId));
      return;
    }

    if (error != null) {
      const errorItems: ErrorItem[] = [
        {
          field: 'message',
          message: error.error.message,
        },
      ];

      if (error.error.details && error.error.details.length > 0) {
        errorItems.push(
          ...error.error.details.map((d) => ({
            field: d.field,
            message: d.message,
          })),
        );
      }

      this.errors.set(errorItems);

      const timeoutId = setTimeout(() => {
        this.errors.set([]);
      }, 5000);

      this.destroyRef.onDestroy(() => clearTimeout(timeoutId));
    } else {
      this.errors.set([
        { field: 'error', message: 'Er is een fout opgetreden.' },
      ]);

      const timeoutId = setTimeout(() => {
        this.errors.set([]);
      }, 5000);

      this.destroyRef.onDestroy(() => clearTimeout(timeoutId));
    }
  }
}