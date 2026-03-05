import { Component } from '@angular/core';
import { BookService } from '../../../services/BookService/book.service';
import { FormsModule } from '@angular/forms';
import { Book } from '../../../model/book.model';
import { Availability } from '../../../model/enums/availability.enum';
import { Route, Router } from '@angular/router';

@Component({
  selector: 'app-add-book-manual',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './add-book-manual.component.html',
  styleUrl: './add-book-manual.component.css'
})
export class AddBookManualComponent {
  book: Book = {
    ISBN: 0,
    title: '',
    author: '',
    pages: 0,
    image_url: '',
    description: '',
    release_year: 0,
    availability: Availability.AVAILABLE,
    copies: 0
  };
  availabilityOptions = Object.values(Availability);


  constructor(private bs: BookService, private rt: Router){}

  addBook() {
    this.bs.createBookWithoutISBN(this.book).subscribe({
      next: () => {
      this.rt.navigate(['/catalog']);
      },
      error: (err) => {
        console.error(err);
      }
    });
  }
}
