import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink, Router } from '@angular/router';
import { ReadingListService, ReadingList, ReadingListBook } from '../../services/reading-list.service';
import { BookService } from '../../services/BookService/book.service';
import { Book } from '../../model/book.model';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

@Component({
    selector: 'app-reading-list-detail',
    standalone: true,
    imports: [CommonModule, RouterLink],
    templateUrl: './reading-list-detail.component.html',
    styleUrl: './reading-list-detail.component.css'
})
export class ReadingListDetailComponent implements OnInit {

    list: ReadingList | null = null;
    booksInList: Book[] = [];
    loading = true;
    error = false;

    constructor(
        private route: ActivatedRoute,
        private router: Router,
        private readingListService: ReadingListService,
        private bookService: BookService
    ) { }

    ngOnInit() {
        const id = Number(this.route.snapshot.paramMap.get('id'));
        this.readingListService.getListById(id).subscribe({
            next: (listData) => {
                this.list = listData;

                if (listData.books && listData.books.length > 0) {
                    // Fetch all books based on bookIds
                    const bookRequests = listData.books.map(b =>
                        this.bookService.getBookById(b.bookId).pipe(
                            catchError(() => of(null)) // ignore missing books
                        )
                    );

                    forkJoin(bookRequests).subscribe((books) => {
                        this.booksInList = books.filter(b => b !== null) as Book[];
                        this.loading = false;
                    });
                } else {
                    this.loading = false;
                }
            },
            error: () => {
                this.error = true;
                this.loading = false;
            }
        });
    }

    goToBook(bookId: number | undefined) {
        if (bookId) {
            this.router.navigate(['/book', bookId]);
        }
    }
}
