import { Component, EventEmitter, Input, NgModule, Output } from '@angular/core';
import { BookService } from '../../../services/BookService/book.service';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Book } from '../../../model/book.model';
import { Availability } from '../../../model/enums/availability.enum';
import { CheckCircle, Info, LucideAngularModule, Plus, Search } from "lucide-angular";

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

  book: Book = {
    title: '',
    author: '',
    pages: 0,
    imageUrl: '',
    description: '',
    releaseYear: new Date().getFullYear(),
  };

  constructor(private bs: BookService, private rt: Router){}

  save() {
    this.bookAdded.emit(this.book);
  }
  
  searchByISBN() {
    this.bs.getBookByISBN(this.isbn).subscribe({
      next: (data) => {
        this.book = data;
      },
      error: (err) => {
        console.error("Boek niet gevonden", err);
      }
    });
  }
}
