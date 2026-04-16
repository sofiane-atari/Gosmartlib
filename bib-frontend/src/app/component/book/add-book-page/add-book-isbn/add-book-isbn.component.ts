import { Component, EventEmitter, Input, NgModule, Output, ViewChild } from '@angular/core';
import { BookService } from '../../../../services/bookService/book.service';
import { Router } from '@angular/router';
import { FormsModule, NgForm } from '@angular/forms';
import { Book } from '../../../../model/book/book.model';
import { Info, LucideAngularModule, Plus, Trash2 } from "lucide-angular";
import { BookCandidate } from '../../../../model/book/book-candidate.model';
import { ToastService } from '../../../../services/toast/toast.service';
import { LoadingOverlayComponent } from "../../../shared/loading-overlay/loading-overlay.component";
import { Genre } from '../../../../model/genre/genre.model';
import { ReadingLevel } from '../../../../model/readingLevel.model';
import { GenreSelectionBase } from '../../../genre/genre-selection.base';

@Component({
  selector: 'app-add-book-isbn',
  standalone: true,
  imports: [FormsModule, LucideAngularModule, LoadingOverlayComponent],
  templateUrl: './add-book-isbn.component.html',
  styleUrl: './add-book-isbn.component.css'

})
export class AddBookISBNComponent extends GenreSelectionBase {
  isbn: string = "";
  selectedBook?: Book;
  @ViewChild('bookForm') bookForm?: NgForm;
  @Output() bookAdded = new EventEmitter<Book>();
  @Input() readingLevels: ReadingLevel[] = [];
  readonly InfoIcon = Info;
  readonly PlusIcon = Plus;
  readonly Trash2Icon = Trash2;

  selectedBookId: number | null = null;
  isExistingBookLoaded = false;
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

  constructor(private bs: BookService, private rt: Router, private ts: ToastService){
    super();
  }

  save() {
    if (!this.isGenreSelectionValid()) {
      this.ts.show('Kies 1 hoofdgenre en selecteer daarvan 1 tot 3 subgenres.', 'error');
      return;
    }

    if (this.isExistingBookLoaded && this.selectedBookId) {
      this.bs.addCopy(this.selectedBookId).subscribe({
        next: () => {
          this.ts.show('Exemplaar toegevoegd.');
          this.clearForm();
        },
        error: () => {
          this.ts.show('Kon geen exemplaar toevoegen.', 'error');
        }
      });
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

    this.ts.show(
      'Zoeken zonder ISBN kan alleen met titel, auteur én jaar samen.',
      'error'
    );
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
        this.openedMainGenre = this.findMainGenreForSelectedGenres();
        this.selectedBookId = data.id ?? null;
        this.isExistingBookLoaded = !!data.id;
        this.isLoading = false;

        this.ts.show(
          this.isExistingBookLoaded
            ? 'Boekgegevens uit de database ingevuld.'
            : 'Boek gevonden.'
        );
      },
      error: () => {
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
    this.openedMainGenre = null;
    this.selectedBookId = null;
    this.isExistingBookLoaded = false;
    this.resetGenreSelection();
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
        this.openedMainGenre = this.findMainGenreForSelectedGenres();
        this.isbn = selectedBook.isbn ?? candidate.isbn13;

        this.selectedBookId = selectedBook.id ?? null;
        this.isExistingBookLoaded = !!selectedBook.id;

        this.showCandidates = false;
        this.candidates = [];
        this.isLoading = false;
        this.ts.show(
          this.isExistingBookLoaded
            ? 'Boekgegevens uit de database ingevuld.'
            : 'Boekgegevens ingevuld.'
        );
      },
      error: () => {
        this.isLoading = false;
        this.ts.show('Kon het gekozen boek niet ophalen.', 'error');
      }
    });
  }
  protected getSelectedGenres(): Genre[] {
    return this.book.genres;
  }

  protected setSelectedGenres(genres: Genre[]): void {
    this.book.genres = genres;
  }

  protected showGenreError(message: string): void {
    this.ts.show(message, 'error');
  }
  compareReadingLevels = (a: ReadingLevel | null, b: ReadingLevel | null): boolean => {
    return a?.id === b?.id;
  };
}
