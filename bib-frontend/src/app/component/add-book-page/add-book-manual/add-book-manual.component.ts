import { Component, EventEmitter, Input, Output } from '@angular/core';
import { BookService } from '../../../services/BookService/book.service';
import { FormsModule } from '@angular/forms';
import { Book } from '../../../model/book.model';
import { Availability } from '../../../model/enums/availability.enum';
import { Route, Router } from '@angular/router';
import { Info, LucideAngularModule, Plus } from "lucide-angular";
import { ToastService } from '../../../services/Toast/toast.service';
import { Genre } from '../../../model/genre.model';

@Component({
  selector: 'app-add-book-manual',
  standalone: true,
  imports: [FormsModule, LucideAngularModule],
  templateUrl: './add-book-manual.component.html',
  styleUrl: './add-book-manual.component.css'
})
export class AddBookManualComponent {
  readonly PlusIcon = Plus;
  availabilityOptions = Object.values(Availability);
    book: Book = {
    title: '',
    author: '',
    pages: 0,
    imageUrl: '',
    description: '',
    releaseYear: new Date().getFullYear(),
    genres: []
  };
  constructor(private bs: BookService, private rt: Router, private ts: ToastService){}

  @Input() genres: Genre[] = [];
  @Output() bookAdded = new EventEmitter<Book>();
  readonly InfoIcon = Info;

  toggleGenre(genreName: string) {
    if (this.book.genres.includes(genreName)) {
      this.book.genres = this.book.genres.filter(g => g !== genreName);
    } else {
      this.book.genres.push(genreName);
    }
  }

  isGenreSelected(genreName: string): boolean {
    return this.book.genres.includes(genreName);
  }

  save() {
    if (this.book.genres.length === 0) {
      this.ts.show('Selecteer minstens 1 genre', 'error')
      return;
    }
    this.bookAdded.emit(this.book);
  }

}
