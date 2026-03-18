import { Component, EventEmitter, Input, NgModule, Output } from '@angular/core';
import { BookService } from '../../../services/BookService/book.service';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Book } from '../../../model/book.model';
import { Availability } from '../../../model/enums/availability.enum';
import { CheckCircle, Info, LucideAngularModule, Plus, Search } from "lucide-angular";
import { BookCandidate } from '../../../model/book-candidate.model';
import { ToastService } from '../../../services/Toast/toast.service';

@Component({
  selector: 'app-add-book-isbn',
  standalone: true,
  imports: [FormsModule, LucideAngularModule],
  templateUrl: './add-book-isbn.component.html',
  styleUrl: './add-book-isbn.component.css'
  
})
export class AddBookISBNComponent {
  isbn: string = "";
  public availabilityOptions = ['AVAILABLE', 'DAMAGED', 'LOST', 'BORROWED'];
  selectedBook?: Book;
  @Output() bookAdded = new EventEmitter<Book>();
  readonly InfoIcon = Info;

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
  
  searchByISBN() {

    this.showCandidates = false;
    this.candidates = [];

    if (!this.isbn || this.isbn.trim().length === 0) {
      this.ts.show('Geef eerst een ISBN in.');
      return;
    }

    this.isLoading = true;
    this.bs.getBookByISBN(this.isbn).subscribe({
      next: (data) => {
        this.book = data;
      },
      error: (err) => {
        this.ts.show('Geen boek gevonden op basis van ISBN.');
      }
    });
  }

  searchByTitleAuthorYear() {
    this.showCandidates = false;
    this.candidates = [];

    if (!this.book.title?.trim() || !this.book.author?.trim() || !this.book.releaseYear) {
      this.ts.show('Vul titel, auteur en jaar in om te zoeken.');
      return;
    }

    this.isLoading = true;

    this.bs.searchBookCandidates(
      this.book.title.trim(),
      this.book.author.trim(),
      this.book.releaseYear
    ).subscribe({
      next: (results: BookCandidate[]) => {
        this.candidates = results;
        this.showCandidates = true;
        this.isLoading = false;

        if (results.length === 0) {
          this.ts.show('Geen boek gevonden.');
          return;
        }

        this.ts.show(`${results.length} boek(en) gevonden.`);
      },
      error: () => {
        this.isLoading = false;
        this.ts.show('Geen boek gevonden voor titel, auteur en jaar.');
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

        this.ts.show('Boek geselecteerd. Controleer de gegevens en klik daarna op "Boek Toevoegen".');
      },
      error: () => {
        this.isLoading = false;
        this.ts.show('Kon het gekozen boek niet ophalen.');
      }
    });
  }
}
