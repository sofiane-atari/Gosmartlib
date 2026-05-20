import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import {ReadingListService} from '../../../services/reading-list/reading-list.service';
import { ToastService } from '../../../services/toast/toast.service';
import { BookService } from '../../../services/bookService/book.service';
import { RoleService } from '../../../services/role-service/role.service';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import {
  LucideAngularModule,
  Share2,
  Trash2,
  List,
  Star,
  User,
  Users,
  BookOpen,
  Book,
  BookCopy,
  LucideIconData
} from 'lucide-angular';
import { ButtonHoverHelpComponent } from '../../shared/button-hover-help/button-hover-help.component';
import { ReadingList, ReadingListTargetType } from '../../../model/readingList.model';

@Component({
  selector: 'app-reading-list-overview',
  standalone: true,
  imports: [CommonModule, RouterLink, LucideAngularModule, ButtonHoverHelpComponent],
  templateUrl: './reading-list-overview.component.html',
  styleUrl: './reading-list-overview.component.css'
})
export class ReadingListOverviewComponent implements OnInit {

  readonly Share2 = Share2;
  readonly Trash2 = Trash2;
  readonly EmptyStateIcon = BookOpen;
  readonly EmptyCoverIcon = Book;
  readonly Star = Star;

  private static readonly FAVORITES_NAME = 'favorieten';
  readingLists: ReadingList[] = [];
  listCovers: { [key: number]: string[] } = {};

  private static readonly FEATURED_TARGET_TYPE: ReadingListTargetType = 'SYSTEM';
  private readonly MAX_CATALOG_FEATURED_LISTS = 2;
  protected readonly BookCopy = BookCopy;

  // Filter state
  selectedFilters: Set<string> = new Set(['ALL']);
  filterOptions: Array<{ id: string; label: string; icon: LucideIconData }> = [
    { id: 'ALL', label: 'Alle', icon: List },
    { id: 'FEATURED', label: 'Uitgelicht', icon: Star },
    { id: 'PERSONAL', label: 'Persoonlijk', icon: User },
    { id: 'CLASS', label: 'Klaslijsten', icon: Users },
  ];

  constructor(
    private readingListService: ReadingListService,
    private toastService: ToastService,
    private bookService: BookService,
    private roleService: RoleService
  ) { }

  get createRoute(): string {
    return this.roleService.isTeacher() ? '/reading-list/create-teacher' : '/reading-list/create';
  }

  ngOnInit() {
    this.readingListService.getLists().subscribe(lists => {
      this.readingLists = lists;
      this.fetchCoversForLists(lists);
    });
  }

  // Filter toggle logic
  toggleFilter(filterId: string): void {
    if (filterId === 'ALL') {
      this.selectedFilters.clear();
      this.selectedFilters.add('ALL');
    } else {
      this.selectedFilters.delete('ALL');
      if (this.selectedFilters.has(filterId)) {
        this.selectedFilters.delete(filterId);
        if (this.selectedFilters.size === 0) {
          this.selectedFilters.add('ALL');
        }
      } else {
        this.selectedFilters.add(filterId);
      }
    }
  }

  // Get filtered lists based on selected filters
  getFilteredLists(): ReadingList[] {
    if (this.selectedFilters.has('ALL')) {
      return this.readingLists;
    }

    return this.readingLists.filter(list => this.matchesSelectedFilters(list));
  }

  // Count lists by type
  getFilterCount(filterId: string): number {
    if (filterId === 'ALL') {
      return this.readingLists.length;
    }

    return this.readingLists.filter(list => this.matchesFilter(list, filterId)).length;
  }

  private matchesSelectedFilters(list: ReadingList): boolean {
    for (const filterId of this.selectedFilters) {
      if (this.matchesFilter(list, filterId)) {
        return true;
      }
    }
    return false;
  }

  private matchesFilter(list: ReadingList, filterId: string): boolean {
    const targetType = list.targetType || 'PERSONAL';

    if (filterId === 'FEATURED') {
      // Featured lists are platform-wide lists visible to everyone.
      return targetType === ReadingListOverviewComponent.FEATURED_TARGET_TYPE;
    }

    return targetType === filterId;
  }

  fetchCoversForLists(lists: ReadingList[]) {
    lists.forEach(list => {
      if (list.books && list.books.length > 0) {
        const booksToFetch = list.books.slice(0, 4);
        const coverRequests = booksToFetch.map(b =>
          this.bookService.getBookById(b.bookId).pipe(
             catchError(() => of(null))
          )
        );

        forkJoin(coverRequests).subscribe(books => {
          const covers = books
            .filter(b => b && b.imageUrl)
            .map(b => b!.imageUrl as string);
          this.listCovers[list.id] = covers;
        });
      } else {
        this.listCovers[list.id] = [];
      }
    });
  }

  getCoverStyle(index: number, total: number): { [klass: string]: any } {
    const zIndex = 10 - index;
    const spacing = 36;
    const totalWidth = (total - 1) * spacing;
    const translateX = (totalWidth / 2) - (index * spacing);
    const scale = 1 - (index * 0.05);

    return {
      'z-index': zIndex,
      'transform': `translateX(${translateX}px) scale(${scale})`
    };
  }

  isFavoritesList(list: ReadingList): boolean {
    return list.name?.trim().toLowerCase() === ReadingListOverviewComponent.FAVORITES_NAME;
  }

  isReadOnlyAudience(list: ReadingList): boolean {
    return !!list.targetType && list.targetType !== 'PERSONAL';
  }

  canDeleteList(list: ReadingList): boolean {
    if (this.isFavoritesList(list)) {
      return false;
    }

    if ((list.targetType ?? 'PERSONAL') === 'SYSTEM') {
      return false;
    }

    return (list.targetType ?? 'PERSONAL') === 'PERSONAL' || this.roleService.isTeacher();
  }

  deleteList(list: ReadingList) {
    if (!this.canDeleteList(list)) {
      this.toastService.show('Deze leeslijst kan niet verwijderd worden', 'error');
      return;
    }

    this.toastService.showConfirm(
      `Weet je zeker dat je '${list.name}' wil verwijderen?`,
      () => {
        this.readingListService.deleteList(list.id).subscribe({
          next: () => {
            this.readingLists = this.readingLists.filter(l => l.id !== list.id);
            this.toastService.show('Leeslijst verwijderd', 'success');
          },
          error: (err) => {
            console.error('Fout bij verwijderen van lijst:', err);
            this.toastService.show('Verwijderen mislukt', 'error');
          }
        });
      }
    );
  }

  shareList(id: number) {
    this.readingListService.shareReadingList(id).subscribe({
      next: (result) => {
        navigator.clipboard.writeText(result.url).then(() => {
          this.toastService.show('Deellink gekopieerd naar klembord!', 'success');
        });
      },
      error: (err) => {
        console.error('Fout bij delen van leeslijst:', err);
        this.toastService.show('Delen van leeslijst mislukt', 'error');
      }
    });
  }
  get catalogFeaturedLists(): ReadingList[] {
    return this.readingLists
      .filter((list) => list.showOnCatalog === true)
      .sort(
        (a, b) =>
          (a.catalogFeaturedOrder ?? Number.MAX_SAFE_INTEGER) -
          (b.catalogFeaturedOrder ?? Number.MAX_SAFE_INTEGER),
      );
  }

  canManageCatalogFeaturedLists(): boolean {
    return this.roleService.canManageReadingLists();
  }

  isCatalogFeatured(list: ReadingList): boolean {
    return list.showOnCatalog === true;
  }

  toggleCatalogFeatured(list: ReadingList, event: Event): void {
    event.stopPropagation();

    if (!this.canManageCatalogFeaturedLists()) {
      this.toastService.show('Je hebt geen rechten om uitgelichte cataloguslijsten te beheren.', 'error');
      return;
    }

    if (list.targetType !== 'SYSTEM') {
      this.toastService.show('Alleen uitgelichte lijsten kunnen op de catalogus getoond worden.', 'error');
      return;
    }

    if (this.isCatalogFeatured(list)) {
      this.readingListService.unselectCatalogFeaturedList(list.id).subscribe({
        next: (selectedLists) => {
          this.syncCatalogFeaturedSelection(selectedLists);
          this.toastService.show('Leeslijst wordt niet meer op de catalogus getoond.', 'success');
        },
        error: () => {
          this.toastService.show('De selectie kon niet worden aangepast.', 'error');
        },
      });

      return;
    }
    const selectedCount = this.catalogFeaturedLists.length;

    if (selectedCount >= this.MAX_CATALOG_FEATURED_LISTS) {
      const firstSelected = this.catalogFeaturedLists
        .slice()
        .sort((a, b) => (a.catalogFeaturedOrder ?? 0) - (b.catalogFeaturedOrder ?? 0))[0];

        this.toastService.showConfirm(
          `Er kunnen maximaal ${this.MAX_CATALOG_FEATURED_LISTS} uitgelichte leeslijsten op de catalogus staan. Als je doorgaat, wordt "${firstSelected.name}" van de catalogus gehaald en wordt "${list.name}" toegevoegd.`,
          () => this.selectCatalogFeaturedList(list),
        );

      return;
    }

    this.selectCatalogFeaturedList(list);
  }

  private selectCatalogFeaturedList(list: ReadingList): void {
    this.readingListService.selectCatalogFeaturedList(list.id).subscribe({
      next: (selectedLists) => {
        this.syncCatalogFeaturedSelection(selectedLists);
        this.toastService.show('Leeslijst wordt nu op de catalogus getoond.', 'success');
      },
      error: () => {
        this.toastService.show('De leeslijst kon niet op de catalogus gezet worden.', 'error');
      },
    });
  }

  private syncCatalogFeaturedSelection(selectedLists: ReadingList[]): void {
    const selectedIds = new Set(selectedLists.map((list) => list.id));
    const orderById = new Map(
      selectedLists.map((list) => [list.id, list.catalogFeaturedOrder ?? null])
    );

    this.readingLists = this.readingLists.map((list) => ({
      ...list,
      showOnCatalog: selectedIds.has(list.id),
      catalogFeaturedOrder: orderById.get(list.id) ?? null,
    }));
  }
}
