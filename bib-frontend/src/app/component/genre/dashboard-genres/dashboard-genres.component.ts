import {
  Component,
  computed,
  inject,
  signal,
  DestroyRef
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import { GenreService } from '../../../services/genre/genre.service';
import { GenresListComponent } from '../genres-list/genres-list.component';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { SearchBarComponent } from '../../shared/search-bar/search-bar.component';
import { PaginatedResponse } from '../../../model/shared/paging/PaginatedResponse.model';
import { ActivatedRoute, Router } from '@angular/router';
import { PagingComponent } from '../../shared/paging/paging.component';
import { switchMap, debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { Subject } from 'rxjs';
import { HttpErrorResponseError } from '../../../error-reponse.model';
import { Genre } from '../../../model/genre/genre.model';
import { ButtonHoverHelpComponent } from '../../shared/button-hover-help/button-hover-help.component';
import { AddSubmitEvent, AddComponent } from '../../shared/add/add.component';
import {LucideAngularModule,
  BookOpen,
  CircleOff,
  Plus,
  Tag 
} from "lucide-angular";
import { StatCardComponent } from "../../shared/stat-card/stat-card.component";
import { fadeInUpAnimation } from '../../shared/animations/my-reviews.animations';

type PopupState = 'create' | 'edit' | null;

interface ErrorItem {
  field: string;
  message: string;
}

@Component({
  selector: 'app-dashboard-genres',
  standalone: true,
  imports: [
    FormsModule,
    SearchBarComponent,
    PagingComponent,
    GenresListComponent,
    AddComponent,
    StatCardComponent,
    LucideAngularModule
],
  templateUrl: './dashboard-genres.component.html',
  styleUrls: ['./dashboard-genres.component.css'],
  animations: [fadeInUpAnimation],
})
export class DashboardGenresComponent {
  private genreService = inject(GenreService);
  private destroyRef = inject(DestroyRef);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private refreshTrigger = signal(0);

  // Track genre being edited
  protected editingGenre = signal<Genre | null>(null);

  // Subject for debounced search
  private searchSubject = new Subject<string>();

  protected success = signal<string | null>(null);
  protected errors = signal<ErrorItem[]>([]);
  protected popUp = signal<PopupState>(null);

  // BookCount filter (0 = any, 1+ = has books, -1 = no books)
  protected bookCountFilter = signal<string>('');
  private readonly defaultGenreColor = '#3b82f6';

  protected readonly Plus = Plus;
  protected readonly Tag = Tag;
  protected readonly BookOpen = BookOpen;
  protected readonly CircleOff = CircleOff;
  

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
  private genres$ = this.route.queryParamMap.pipe(
    switchMap((queryParams) => {
      const page = Number(queryParams.get('page')) || 0;
      const pageSize = Number(queryParams.get('pageSize')) || 10;
      const search = queryParams.get('search') || undefined;
      const bookCount = queryParams.get('bookCount') || undefined;

      return this.genreService.getGenresByPage({
        genreGetOpt: {
          includeBookCount: true,
          search,
          bookCount,
        },
        pagingOpt: { page, pageSize },
      });
    }),
  );
  private response = toSignal(
    toObservable(this.refreshTrigger).pipe(switchMap(() => this.genres$)),
    {
      initialValue: {
        data: [],
        meta: { currentPage: 0, pageSize: 20, totalItems: 0, totalPages: 0 },
      } as PaginatedResponse<never>,
    },
  );
  protected stats = toSignal(
    toObservable(this.refreshTrigger).pipe(
      switchMap(() => this.genreService.getGenreStats()),
    ),
  );
  protected totalActive = computed(
    () => (this.stats()?.totalItems ?? 0) - (this.stats()?.unActiveGenres ?? 0),
  );

  protected genres = computed(() => this.response()?.data ?? []);
  protected pagingMeta = computed(() => this.response()?.meta);

  protected onSearch(query: string): void {
    // Push to subject for debouncing
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

  protected setPopup(popup: PopupState) {
    this.popUp.set(popup);
  }

  protected closePopup(): void {
    this.setPopup(null);
    this.editingGenre.set(null);
  }

  protected onEditGenre(genre: Genre): void {
    console.log('Genre geselecteerd voor bewerken:', genre);
    this.editingGenre.set(genre);
    this.setPopup('edit');
  }

  protected onSuccess(message: string) {
    this.success.set(message);

    this.refresh();

    this.closePopup();

    const timeoutId = setTimeout(() => {
      this.success.set(null);
    }, 5000);

    this.destroyRef.onDestroy(() => clearTimeout(timeoutId));
  }

  protected onError(error: string | HttpErrorResponseError) {
    this.refresh();

    // Handle string errors from CreateLabelComponent
    if (typeof error === 'string') {
      this.errors.set([{ field: 'error', message: error }]);

      // Auto-dismiss after 5 seconds
      const timeoutId = setTimeout(() => {
        this.errors.set([]);
      }, 5000);
      this.destroyRef.onDestroy(() => clearTimeout(timeoutId));

      return;
    }

    // Handle HttpErrorResponseError from LabelsListComponent
    if (error != null) {
      const errorItems: ErrorItem[] = [
        { field: 'message', message: error.error.message },
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

      // Auto-dismiss after 5 seconds
      const timeoutId = setTimeout(() => {
        this.errors.set([]);
      }, 5000);
      this.destroyRef.onDestroy(() => clearTimeout(timeoutId));
    } else {
      this.errors.set([
        { field: 'error', message: 'Er is een fout opgetreden.' },
      ]);

      // Auto-dismiss after 5 seconds
      const timeoutId = setTimeout(() => {
        this.errors.set([]);
      }, 5000);
      this.destroyRef.onDestroy(() => clearTimeout(timeoutId));
    }
  }

  protected openCreatePopup(): void {
    this.editingGenre.set(null);
    this.setPopup('create');
  }

  protected onSubmitCreateGenre(event: AddSubmitEvent): void {
    if (!event.name) {
      this.onError('Naam is verplicht.');
      return;
    }

    this.genreService
      .createGenre({
        name: event.name,
        description: event.description,
        color: event.color ?? this.defaultGenreColor,
      })
      .subscribe({
        next: () => {
          this.onSuccess('Genre succesvol aangemaakt.');
        },
        error: (error: HttpErrorResponseError) => {
          this.onError(error);
        },
      });
  }

protected onSubmitUpdateGenre(event: AddSubmitEvent): void {
  const genre = this.editingGenre();

  if (!genre) {
    this.onError('Geen genre geselecteerd om te bewerken.');
    return;
  }

  if (!event.name) {
    this.onError('Naam is verplicht.');
    return;
  }

  this.genreService
    .updateGenre({
      id: genre.id!,
      name: event.name,
      description: event.description,
      color: event.color ?? genre.color,
    })
    .subscribe({
      next: () => {
        this.onSuccess('Genre succesvol bijgewerkt.');
      },
      error: (error: HttpErrorResponseError) => {
        this.onError(error);
      },
    });
}

  private refresh(): void {
    this.refreshTrigger.update((value) => value + 1);
  }
}
