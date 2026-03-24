import { Component, EventEmitter, Input, Output } from '@angular/core';
import { BookService } from '../../../services/BookService/book.service';
import { FormsModule } from '@angular/forms';
import { Book } from '../../../model/book.model';
import { Availability } from '../../../model/enums/availability.enum';
import { Route, Router } from '@angular/router';
import { Info, LucideAngularModule, Plus } from "lucide-angular";
import { ToastService } from '../../../services/toast/toast.service';
import { Genre } from '../../../model/genre.model';
import { ReadingLevel } from '../../../model/readingLevel.model';

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
    genres: [],
    readingLevel: undefined 
  };
  constructor(private bs: BookService, private rt: Router, private ts: ToastService){}

  @Input() genres: Genre[] = [];
  @Input() readingLevels: ReadingLevel[] = [];
  @Output() bookAdded = new EventEmitter<Book>();
  readonly InfoIcon = Info;

  toggleGenre(genre: Genre) {
    const exists = this.book.genres.some(g => g.name === genre.name);

    if (exists) {
      this.book.genres = this.book.genres.filter(g => g.name !== genre.name);
    } else {
      this.book.genres.push(genre);
    }
  }

  isGenreSelected(genre: Genre): boolean {
    return this.book.genres.some(g => g.name  === genre.name);
  }

  save() {
    if (this.book.genres.length === 0) {
      this.ts.show('Selecteer minstens 1 genre', 'error')
      return;
    }

    this.bookAdded.emit(this.book);
  }
  

}
