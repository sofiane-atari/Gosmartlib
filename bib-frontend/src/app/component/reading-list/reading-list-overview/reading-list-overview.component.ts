import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ReadingListService, ReadingList } from '../../../services/reading-list/reading-list.service';
import { ToastService } from '../../../services/toast/toast.service';
import { BookService } from '../../../services/bookService/book.service';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { LucideAngularModule, Share2, Trash2 } from 'lucide-angular';

@Component({
  selector: 'app-reading-list-overview',
  standalone: true,
  imports: [CommonModule, RouterLink, LucideAngularModule],
  templateUrl: './reading-list-overview.component.html',
  styleUrl: './reading-list-overview.component.css'
})
export class ReadingListOverviewComponent implements OnInit {

  readonly Share2 = Share2;
  readonly Trash2 = Trash2;

  private static readonly FAVORITES_NAME = 'favorieten';
  readingLists: ReadingList[] = [];
  listCovers: { [key: number]: string[] } = {};

  constructor(
    private readingListService: ReadingListService,
    private toastService: ToastService,
    private bookService: BookService
  ) { }

  ngOnInit() {
    this.readingListService.getLists().subscribe(lists => {
      this.readingLists = lists;
      this.fetchCoversForLists(lists);
    });
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
    return !this.isFavoritesList(list) && !this.isReadOnlyAudience(list);
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
    const url = `${window.location.origin}/reading-list/${id}`;
    navigator.clipboard.writeText(url).then(() => {
      this.toastService.show('Link gekopieerd naar klembord!', 'success');
    });
  }

}
