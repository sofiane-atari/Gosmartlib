import {
  Component,
  computed,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap } from 'rxjs/operators';

import {
  LucideAngularModule,
  Plus,
  Tag as TagIcon,
  CheckCircle,
  CircleOff,
  Pencil,
  Trash2,
} from 'lucide-angular';

import { TagService } from '../../../services/tag/tag.service';
import { ToastService } from '../../../services/toast/toast.service';

import { Tag as TagModel } from '../../../model/tag/tag.model';
import { PaginatedResponse } from '../../../model/shared/paging/PaginatedResponse.model';
import { HttpErrorResponseError } from '../../../error-reponse.model';

import { PagingComponent } from '../../shared/paging/paging.component';
import { SearchBarComponent } from '../../shared/search-bar/search-bar.component';
import { ToastComponent } from '../../shared/toast/toast.component';
import { HttpErrorResponse } from '@angular/common/http';
import { ButtonHoverHelpComponent } from "../../shared/button-hover-help/button-hover-help.component";
import { AddSubmitEvent, AddComponent } from '../../shared/add/add.component';
import { StatCardComponent } from "../../shared/stat-card/stat-card.component";
import { DataTableComponent } from '../../shared/data-table/data-table.component';
import { CellTemplateDirective } from '../../shared/data-table/cell-template.directive';
import { DataTableColumn } from '../../shared/data-table/data-table.types';

type PopupState = 'create' | 'edit' | null;

@Component({
  selector: 'app-tag-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    LucideAngularModule,
    PagingComponent,
    SearchBarComponent,
    ButtonHoverHelpComponent,
    AddComponent,
    StatCardComponent,
    DataTableComponent,
    CellTemplateDirective,
],
  templateUrl: './tag-dashboard.component.html',
  styleUrls: ['./tag-dashboard.component.css'],
})
export class TagDashboardComponent {
  protected readonly Plus = Plus;
  protected readonly TagIcon = TagIcon;
  protected readonly CheckCircle = CheckCircle;
  protected readonly CircleOff = CircleOff;
  protected readonly Pencil = Pencil;
  protected readonly Trash2 = Trash2;

  private refreshTrigger = signal(0);
  private searchSubject = new Subject<string>();

  protected popUp = signal<PopupState>(null);
  protected editingTag = signal<TagModel | null>(null);

  protected bookCountFilter = signal<string>('');

  private backdropPointerStartedOnOverlay = false;

  private readonly tagService = inject(TagService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected readonly columns: DataTableColumn[] = [
    { id: 'name', header: 'Label', dataLabel: 'Label' },
    { id: 'description', header: 'Beschrijving', dataLabel: 'Beschrijving' },
    { id: 'bookCount', header: 'Boeken', dataLabel: 'Boeken' },
    { id: 'actions', header: 'Acties', dataLabel: 'Acties', align: 'right' },
  ];

  constructor(private readonly toastService: ToastService) {
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
        pagingOpt: {
          page,
          pageSize,
        },
      });
    }),
  );

  private response = toSignal(
    toObservable(this.refreshTrigger).pipe(
      switchMap(() => this.tags$),
    ),
    {
      initialValue: {
        data: [],
        meta: {
          currentPage: 0,
          pageSize: 10,
          totalItems: 0,
          totalPages: 0,
        },
      } as PaginatedResponse<TagModel>,
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
  protected pagingMeta = computed(() => this.response()?.meta ?? null);

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

  protected openCreatePopup(): void {
    this.popUp.set('create');
  }

  protected openEditPopup(tag: TagModel): void {
    this.editingTag.set(tag);
    this.popUp.set('edit');
  }

  protected closePopup(): void {
    this.popUp.set(null);
    this.editingTag.set(null);
  }

  protected onSubmitCreateTag(event: AddSubmitEvent): void {
    if (!event.name) {
      this.toastService.show('Naam is verplicht.', 'error');
      return;
    }

    this.tagService
      .createTag({
        name: event.name,
        description: event.description,
      })
      .subscribe({
        next: () => {
          this.toastService.show('Tag succesvol aangemaakt.', 'success');
          this.refresh();
          this.closePopup();
        },
        error: (error: HttpErrorResponseError) => {
          this.handleError(error);
        },
      });
  }

  protected onSubmitUpdateTag(event: AddSubmitEvent): void {
    const tag = this.editingTag();

    if (!tag) {
      this.toastService.show('Geen tag geselecteerd om te bewerken.', 'error');
      return;
    }

    if (!event.name) {
      this.toastService.show('Naam is verplicht.', 'error');
      return;
    }

    this.tagService
      .updateTag(tag.id, {
        name: event.name,
        description: event.description,
      })
      .subscribe({
        next: () => {
          this.toastService.show('Tag succesvol bijgewerkt.', 'success');
          this.refresh();
          this.closePopup();
        },
        error: (error: HttpErrorResponseError) => {
          this.handleError(error);
        },
      });
  }

  protected deleteTag(tag: TagModel): void {
    this.toastService.showConfirm(
      `Weet je zeker dat je de tag "${tag.name}" wilt verwijderen?`,
      () => {
        this.tagService.deleteTag(tag.id).subscribe({
          next: () => {
            this.toastService.show('Tag succesvol verwijderd.', 'success');
            this.refresh();
          },
          error: (err: HttpErrorResponse) => {
            this.handleError(err?.error as HttpErrorResponseError);
          },
        });
      },
      () => {
        // Cancel action - do nothing
      },
    );
  }

  protected onOverlayPointerDown(event: PointerEvent): void {
    this.backdropPointerStartedOnOverlay = event.target === event.currentTarget;
  }

  protected onOverlayClick(
    event: MouseEvent,
    modalType: 'create' | 'edit',
  ): void {
    const endedOnOverlay = event.target === event.currentTarget;

    if (!(this.backdropPointerStartedOnOverlay && endedOnOverlay)) {
      this.backdropPointerStartedOnOverlay = false;
      return;
    }

    this.backdropPointerStartedOnOverlay = false;

    if (modalType === 'create' || modalType === 'edit') {
      this.closePopup();
    }
  }

  private refresh(): void {
    this.refreshTrigger.update((value) => value + 1);
  }

  private handleError(error: HttpErrorResponseError): void {
    const errorBody = error?.error;

    if (Array.isArray(errorBody?.details) && errorBody.details.length > 0) {
      const messages = errorBody.details
        .map((detail) => detail?.message)
        .filter(Boolean)
        .join('\n');

      this.toastService.show(messages || 'Er is een fout opgetreden.', 'error');
      return;
    }

    if (errorBody?.message) {
      this.toastService.show(errorBody.message, 'error');
      return;
    }

    this.toastService.show('Er is een fout opgetreden.', 'error');
  }
}
