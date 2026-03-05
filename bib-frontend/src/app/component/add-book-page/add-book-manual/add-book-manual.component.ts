import { Component } from '@angular/core';
import { BookService } from '../../../services/BookService/book.service';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-add-book-manual',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './add-book-manual.component.html',
  styleUrl: './add-book-manual.component.css'
})
export class AddBookManualComponent {
  book: any = {};

  constructor(private bs: BookService){}

  addBook(){
    this.bs.createBookWithoutISBN(this.book).subscribe({
      next: () => console.log("Book added")
    });
  }
}
