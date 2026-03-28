import { CommonModule } from '@angular/common';
import { Component, input, output } from '@angular/core';
import { Book } from '../../../../model/book/book.model';

@Component({
  selector: 'app-book-selection-panel',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './book-selection-panel.component.html',
  styleUrl: './book-selection-panel.component.css',
})
export class BookSelectionPanelComponent {
  public readonly hasSelectedUser = input.required<boolean>();
  public readonly selectedBooksCount = input.required<number>();
  public readonly searchQuery = input.required<string>();
  public readonly searchIsbn = input.required<string>();
  public readonly isLoadingBooks = input.required<boolean>();
  public readonly hasBookSearchInput = input.required<boolean>();
  public readonly filteredBooks = input.required<Book[]>();

  public readonly searchQueryChanged = output<Event>();
  public readonly isbnSearchChanged = output<Event>();
  public readonly isbnKeydown = output<KeyboardEvent>();
  public readonly addBookRequested = output<Book>();
}
