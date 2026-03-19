import { Component, EventEmitter, Input, Output } from '@angular/core';
import { BookService } from '../../../services/BookService/book.service';
import { FormsModule } from '@angular/forms';
import { Book } from '../../../model/book.model';
import { Availability } from '../../../model/enums/availability.enum';
import { Route, Router } from '@angular/router';
import { Info, LucideAngularModule, Plus } from "lucide-angular";
import { ToastService } from '../../../services/Toast/toast.service';

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

  @Input() genres: string[] = [];
  @Output() bookAdded = new EventEmitter<Book>();
  readonly InfoIcon = Info;

  toggleGenre(genre: string) {
    if (this.book.genres.includes(genre)) {
      this.book.genres = this.book.genres.filter(g => g !== genre);
    } else {
      this.book.genres.push(genre);
    }
  }

  isGenreSelected(genre: string): boolean {
    return this.book.genres.includes(genre);
  }

  save() {
    if (this.book.genres.length === 0) {
      this.ts.show('Selecteer minstens 1 genre', 'error')
      return;
    }
    this.bookAdded.emit(this.book);
  }

}
