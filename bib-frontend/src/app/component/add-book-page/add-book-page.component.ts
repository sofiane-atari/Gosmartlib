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
  isLoadingGenres = false;

  constructor(private bs: BookService, private rt: Router, private gs: GenreService){}
  
    book: Book = {
    title: '',
    author: '',
    pages: 0,
    imageUrl: '',
    description: '',
    releaseYear: new Date().getFullYear(),
    genres: []
  };

  ngOnInit(): void {
    this.loadGenres();
  }

  loadGenres(): void {
    this.isLoadingGenres = true;

    this.gs.getLabels().subscribe({
      next: (labels: Genre[]) => {
        this.genres = labels;
        this.isLoadingGenres = false;
      },
      error: (err) => {
        this.isLoadingGenres = false;
      }
    });
  }
  addBook(event: Book) {
  const bookToSave = event ?? this.book; // gebruik event als het bestaat, anders fallback

    this.bs.addBook(bookToSave).subscribe({
      next: () => {
      this.rt.navigate(['/catalog']);
      },
      error: (err) => {
        console.error(err);
      }
    });
  }
}
