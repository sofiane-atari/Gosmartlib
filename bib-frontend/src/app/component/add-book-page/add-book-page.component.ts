import { Component } from '@angular/core';
import { AddBookISBNComponent } from "./add-book-isbn/add-book-isbn.component";
import { AddBookManualComponent } from "./add-book-manual/add-book-manual.component";
import { AddBookBulkComponent } from "./add-book-bulk/add-book-bulk.component"
import { Book } from '../../model/book.model';
import { Availability } from '../../model/enums/availability.enum';
import { BookService } from '../../services/BookService/book.service';
import { Router } from '@angular/router';
import { GenreService } from '../../services/genre/genre.service';
import { Genre } from '../../model/genre.model';
import { ReadingLevelsService } from '../../services/ReadingLevels/reading-levels.service';
import { ReadingLevel } from '../../model/readingLevel.model';

@Component({
  selector: 'app-add-book-page',
  standalone: true,
  imports: [AddBookISBNComponent, AddBookManualComponent, AddBookBulkComponent],
  templateUrl: './add-book-page.component.html',
  styleUrl: './add-book-page.component.css'
})
export class AddBookPageComponent {
mode: 'isbn' | 'manual' | 'bulk' = 'isbn';
  genres: Genre[] = [];
  readingLevels: ReadingLevel[] = [];
  isLoadingGenres = false;
  isLoadingReadingLevels = false;

  constructor(private bs: BookService, private rt: Router, private gs: GenreService, private rls: ReadingLevelsService){}
  
    book: Book = {
    title: '',
    author: '',
    pages: 0,
    imageUrl: '',
    description: '',
    releaseYear: new Date().getFullYear(),
    genres: [],
    readingLevel: undefined 
  };

  ngOnInit(): void {
    this.loadGenres();
    this.loadReadingLevels();
  }

  loadReadingLevels(): void {
    this.isLoadingReadingLevels = true;

    this.rls.getAll().subscribe({
      next: (rl: ReadingLevel[]) => {
        this.readingLevels = rl;
        this.isLoadingReadingLevels = false;
      },
      error: (err) => {
        this.isLoadingReadingLevels = false;
      }
    });
  }
  loadGenres(): void {
    this.isLoadingGenres = true;

    this.gs.getGenres().subscribe({
      next: (genres: Genre[]) => {
        this.genres = genres;
        this.isLoadingGenres = false;
      },
      error: (err) => {
        this.isLoadingGenres = false;
      }
    });
  }
  addBook(event: Book) {
    const bookToSave = event ?? this.book;

    this.bs.addBook(bookToSave).subscribe({
      next: () => {
        localStorage.removeItem('bookFilters');
        this.rt.navigate(['/catalog']);
      },
      error: (err) => {
        console.error(err);
      }
    });
  }
}
