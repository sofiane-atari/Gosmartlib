import { Component, Input } from '@angular/core';
import { BookService } from '../../../services/BookService/book.service';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Book } from '../../../model/book.model';
import { Availability } from '../../../model/enums/availability.enum';

@Component({
  selector: 'app-add-book-isbn',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './add-book-isbn.component.html',
  styleUrl: './add-book-isbn.component.css'
})
export class AddBookISBNComponent {
  isbn: string = "";
  @Input() book!: Book;

  constructor(private bs: BookService, private rt: Router){}
  
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
