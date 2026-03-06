import { Component } from '@angular/core';
import { AddBookISBNComponent } from "./add-book-isbn/add-book-isbn.component";
import { AddBookManualComponent } from "./add-book-manual/add-book-manual.component";
import { Book } from '../../model/book.model';
import { Availability } from '../../model/enums/availability.enum';

@Component({
  selector: 'app-add-book-page',
  standalone: true,
  imports: [AddBookISBNComponent, AddBookManualComponent],
  templateUrl: './add-book-page.component.html',
  styleUrl: './add-book-page.component.css'
})
export class AddBookPageComponent {
mode: 'isbn' | 'manual' = 'isbn';

  public book: Book = {
    title: '',
    author: '',
    pages: 0,
    image_url: '',
    description: '',
    release_year: 0,
    availability: Availability.AVAILABLE,
    copies: 0
  };
}
