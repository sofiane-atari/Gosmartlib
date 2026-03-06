import { Component, EventEmitter, Input, Output } from '@angular/core';
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

  availabilityOptions = Object.values(Availability);
  @Input() book!: Book;
  @Output() bookAdded = new EventEmitter<Book>();
  
  save() {
    this.bookAdded.emit(this.book);
  }

  constructor(private bs: BookService, private rt: Router){}
}
