import { Component, EventEmitter, Input, NgModule, Output, ViewChild } from '@angular/core';
import { BookService } from '../../../../services/bookService/book.service';
import { Router } from '@angular/router';
import { FormsModule, NgForm } from '@angular/forms';
import { Book } from '../../../../model/book/book.model';
import { Info, LucideAngularModule, Plus, Trash2 } from "lucide-angular";
import { BookCandidate } from '../../../../model/book/book-candidate.model';
import { ToastService } from '../../../../services/toast/toast.service';
import { LoadingOverlayComponent } from "../../../shared/loading-overlay/loading-overlay.component";
import { Genre } from '../../../../model/genre.model';
import { ReadingLevel } from '../../../../model/readingLevel.model';

@Component({
  selector: 'app-add-book-isbn',
  standalone: true,
  imports: [FormsModule, LucideAngularModule, LoadingOverlayComponent],
  templateUrl: './add-book-isbn.component.html',
  styleUrl: './add-book-isbn.component.css'

})
export class AddBookISBNComponent {
  isbn: string = "";
  selectedBook?: Book;
  @ViewChild('bookForm') bookForm?: NgForm;
  @Output() bookAdded = new EventEmitter<Book>();
  @Input() genres: Genre[] = [];
  @Input() readingLevels: ReadingLevel[] = [];
  readonly InfoIcon = Info;
  readonly PlusIcon = Plus;
  readonly Trash2Icon = Trash2;


  candidates: BookCandidate[] = [];
  showCandidates = false;
  isLoading = false;

    book: Book = {
    title: '',
    author: '',
    pages: '' as any,
    imageUrl: '',
    description: '',
    releaseYear: null as any,
    genres: [],
    readingLevel: null as any
  };

  constructor(private bs: BookService, private rt: Router, private ts: ToastService){}

  toggleGenre(genre: Genre) {
    const exists = this.book.genres.some(g => g.name === genre.name);

    if (exists) {
      this.book.genres = this.book.genres.filter(g => g.name !== genre.name);
    } else {
      this.book.genres.push(genre);
    }
  }

  isGenreSelected(genre: Genre): boolean {
    return this.book.genres.some(g => g.name  === genre.name);
  }

  save() {

    if (this.book.genres.length === 0) {
      this.ts.show('Selecteer minstens 1 genre', 'error')
      return;
    }

    this.bookAdded.emit(this.book);
  }
  searchBookData() {
  const normalizedIsbn = (this.isbn ?? '').replace(/[-\s]/g, '').trim();
  this.isLoading = true;
    if (normalizedIsbn.length === 13) {
      this.isbn = normalizedIsbn;
      this.searchByISBN();
      return;
    }

    if (
      this.book.title?.trim() &&
      this.book.author?.trim() &&
      this.book.releaseYear
    ) {
      this.searchByTitleAuthorYear();
      return;
    }

    this.ts.show('Vul een ISBN in of vul titel, auteur en jaar in.', 'error');
  }

  searchByISBN() {

    this.showCandidates = false;
    this.candidates = [];
    const normalizedIsbn = (this.isbn ?? '').replace(/[-\s]/g, '').trim();

    if (normalizedIsbn.length !== 13) {
      this.ts.show('ISBN moet uit 13 cijfers bestaan.', 'error');
      return;
    }

    this.isLoading = true;
    this.bs.getBookByISBN(this.isbn).subscribe({
      next: (data) => {
        this.book = data;
        this.isLoading = false;
        this.ts.show('Boek gevonden.');
      },
      error: (err) => {
        this.isLoading = false;
        this.ts.show('Geen boek gevonden op basis van ISBN.', 'error');
      }
    });
  }
clearForm() {
    this.isbn = '';
    this.selectedBook = undefined;
    this.candidates = [];
    this.showCandidates = false;
    this.isLoading = false;

    this.book = {
      title: '',
      author: '',
      pages: 0,
      imageUrl: '',
      description: '',
      releaseYear: new Date().getFullYear(),
      genres: [],
      readingLevel: undefined
    };

    this.bookForm?.resetForm({
      isbn: '',
      title: '',
      author: '',
      pages: 0,
      releaseYear: new Date().getFullYear(),
      imageUrl: '',
      description: '',
      readingLevel: undefined
    });
  }
  searchByTitleAuthorYear() {
    this.showCandidates = false;
    this.candidates = [];

    if (!this.book.title?.trim() || !this.book.author?.trim() || !this.book.releaseYear) {
      this.ts.show('Vul titel, auteur en jaar in om te zoeken.', 'error');
      return;
    }

    this.isLoading = true;

    this.bs.searchBookCandidates(
      this.book.title.trim(),
      this.book.author.trim(),
      this.book.releaseYear
    ).subscribe({
      next: (results: BookCandidate[]) => {
        if (results.length === 0) {
          this.isLoading = false;
          this.ts.show('Geen boek gevonden.', 'error');
          return;
        }

        if (results.length === 1) {
          this.ts.show('1 boek gevonden. Gegevens worden automatisch ingevuld.');
          this.selectCandidate(results[0]);
          return;
        }
        this.candidates = results;
        this.showCandidates = true;
        this.isLoading = false;

        this.ts.show(`${results.length} boek(en) gevonden.`);
      },
      error: () => {
        this.isLoading = false;
        this.ts.show('Geen boek gevonden', 'error');
      }
    });
  }

  selectCandidate(candidate: BookCandidate) {
    this.isLoading = true;

    this.bs.selectBook(candidate.isbn13).subscribe({
      next: (selectedBook: Book) => {
        this.book = selectedBook;
        this.isbn = selectedBook.isbn ?? candidate.isbn13;
        this.showCandidates = false;
        this.candidates = [];
        this.isLoading = false;
        this.ts.show('Boekgegevens ingevuld.');
      },
      error: () => {
        this.isLoading = false;
        this.ts.show('Kon het gekozen boek niet ophalen.', 'error');
      }
    });
  }
}
