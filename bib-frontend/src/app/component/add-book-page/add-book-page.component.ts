import { Component } from '@angular/core';
import { AddBookISBNComponent } from "./add-book-isbn/add-book-isbn.component";
import { AddBookManualComponent } from "./add-book-manual/add-book-manual.component";

@Component({
  selector: 'app-add-book-page',
  standalone: true,
  imports: [AddBookISBNComponent, AddBookManualComponent],
  templateUrl: './add-book-page.component.html',
  styleUrl: './add-book-page.component.css'
})
export class AddBookPageComponent {
mode: 'isbn' | 'manual' = 'isbn';

}
