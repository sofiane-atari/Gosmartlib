import { Component, EventEmitter, Input, NgModule, Output } from '@angular/core';
import { BookService } from '../../../services/BookService/book.service';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Book } from '../../../model/book.model';
import { Availability } from '../../../model/enums/availability.enum';
import { CheckCircle, Info, LucideAngularModule, Plus, Search } from "lucide-angular";
import { BookCandidate } from '../../../model/book-candidate.model';
import { ToastService } from '../../../services/Toast/toast.service';
import { LoadingOverlayComponent } from "../../shared/loading-overlay/loading-overlay.component";

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
  @Output() bookAdded = new EventEmitter<Book>();
  readonly InfoIcon = Info;
  readonly PlusIcon = Plus;


  candidates: BookCandidate[] = [];
  showCandidates = false;
  isLoading = false;

  book: Book = {
    title: '',
    author: '',
    pages: 0,
    imageUrl: '',
    description: '',
    releaseYear: new Date().getFullYear(),
  };

  constructor(private bs: BookService, private rt: Router, private ts: ToastService){}

  save() {
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
        this.isbn = selectedBook.ISBN ?? candidate.isbn13;
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
