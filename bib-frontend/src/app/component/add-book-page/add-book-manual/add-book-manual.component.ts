import { Component, EventEmitter, Input, Output } from '@angular/core';
import { BookService } from '../../../services/BookService/book.service';
import { FormsModule } from '@angular/forms';
import { Book } from '../../../model/book.model';
import { Availability } from '../../../model/enums/availability.enum';
import { Route, Router } from '@angular/router';
import { Info, LucideAngularModule } from "lucide-angular";

@Component({
  selector: 'app-add-book-manual',
  standalone: true,
  imports: [FormsModule, LucideAngularModule],
  templateUrl: './add-book-manual.component.html',
  styleUrl: './add-book-manual.component.css'
})
export class AddBookManualComponent {

  availabilityOptions = Object.values(Availability);
    book: Book = {
    title: '',
    author: '',
    pages: 0,
    imageUrl: '',
    description: '',
    releaseYear: new Date().getFullYear(),
  };

  @Output() bookAdded = new EventEmitter<Book>();
  readonly InfoIcon = Info;

  save() {
    this.bookAdded.emit(this.book);
  }

  constructor(private bs: BookService, private rt: Router){}
}
