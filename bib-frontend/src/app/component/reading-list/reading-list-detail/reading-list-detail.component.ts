import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink, Router } from '@angular/router';
import { ReadingListService, ReadingList } from '../../../services/reading-list/reading-list.service';
import { BookService } from '../../../services/bookService/book.service';
import { Book } from '../../../model/book/book.model';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { ToastService } from '../../../services/toast/toast.service';
import { LucideAngularModule, Trash2 } from 'lucide-angular';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../../services/authorization/auth.service';

@Component({
    selector: 'app-reading-list-detail',
    standalone: true,
    imports: [CommonModule, RouterLink, LucideAngularModule],
    templateUrl: './reading-list-detail.component.html',
    styleUrl: './reading-list-detail.component.css'
})
export class ReadingListDetailComponent implements OnInit {

    readonly Trash2 = Trash2;
    list: ReadingList | null = null;
    booksInList: Book[] = [];
    loading = true;
    error = false;
    isSharedView = false;

    constructor(
        private route: ActivatedRoute,
        private router: Router,
        private readingListService: ReadingListService,
        private bookService: BookService,
        private toastService: ToastService,
        private authService: AuthService
    ) { }

  ngOnInit() {
    this.authService.fetchCurrentUser();

    this.isSharedView = this.route.snapshot.data['shared'] === true;

    if (this.isSharedView) {
      const publicShareId = this.route.snapshot.paramMap.get('publicShareId');

      if (!publicShareId) {
        this.error = true;
        this.loading = false;
        return;
      }

      this.readingListService.getSharedList(publicShareId).subscribe({
        next: (listData) => this.loadListBooks(listData),
        error: () => {
          this.error = true;
          this.loading = false;
        }
      });

      return;
    }

    const id = Number(this.route.snapshot.paramMap.get('id'));

    this.readingListService.getListById(id).subscribe({
      next: (listData) => this.loadListBooks(listData),
      error: () => {
        this.error = true;
        this.loading = false;
      }
    });
  }

  isReadOnlyAudience(): boolean {
    return this.isSharedView || !this.canModifyList();
  }

    private canModifyList(): boolean {
        const target = this.list?.targetType ?? 'PERSONAL';
        if (target === 'PERSONAL') {
            return true;
        }
        if (target === 'SYSTEM') {
            return false;
        }
        return this.authService.currentUser()?.role === 'TEACHER';
    }

    removeBookFromList(bookId: number | undefined, event: MouseEvent) {
        event.stopPropagation();

        if (!this.list?.id || !bookId) {
            return;
        }

        if (this.isReadOnlyAudience()) {
            if ((this.list?.targetType ?? 'PERSONAL') === 'SYSTEM') {
                this.toastService.show('De systeembrede leeslijst kan niet aangepast worden', 'error');
            } else {
                this.toastService.show('Alleen leerkrachten kunnen deze leeslijst aanpassen', 'error');
            }
            return;
        }

        this.toastService.showConfirm(
            'Weet je zeker dat je dit boek uit de leeslijst wil verwijderen?',
            () => {
                this.readingListService.removeBookFromList(this.list!.id, bookId).subscribe({
                    next: () => {
                        this.booksInList = this.booksInList.filter(book => book.id !== bookId);
                        if (this.list?.books) {
                            this.list.books = this.list.books.filter(link => link.bookId !== bookId);
                        }
                        this.toastService.show('Boek verwijderd uit leeslijst', 'success');
                    },
                    error: (err: unknown) => {
                        const message = this.extractBackendError(err) ?? 'Boek verwijderen mislukt';
                        this.toastService.show(message, 'error');
                    }
                });
            }
        );
    }

    private extractBackendError(err: unknown): string | null {
        if (err instanceof HttpErrorResponse) {
            if (typeof err.error === 'string' && err.error.trim().length > 0) {
                return err.error;
            }
            if (err.message && err.message.trim().length > 0) {
                return err.message;
            }
        }
        return null;
    }

    goToBook(bookId: number | undefined) {
        if (bookId) {
            this.router.navigate(['/book', bookId]);
        }
    }

  private loadListBooks(listData: ReadingList): void {
    this.list = listData;

    if (listData.books && listData.books.length > 0) {
      const bookRequests = listData.books.map(b =>
        this.bookService.getBookById(b.bookId).pipe(
          catchError(() => of(null))
        )
      );

      forkJoin(bookRequests).subscribe((books) => {
        this.booksInList = books.filter(b => b !== null) as Book[];
        this.loading = false;
      });
    } else {
      this.loading = false;
    }
  }
}
