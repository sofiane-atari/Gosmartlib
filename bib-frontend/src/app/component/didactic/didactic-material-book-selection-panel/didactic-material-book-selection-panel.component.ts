// didactic-material-book-selection-panel.component.ts
import { CommonModule } from '@angular/common';
import { Component, input, output } from '@angular/core';

import { BookCandidate } from '../../../model/book/book-candidate.model';

@Component({
  selector: 'app-didactic-material-book-selection-panel',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './didactic-material-book-selection-panel.component.html',
  styleUrl: './didactic-material-book-selection-panel.component.css',
})
export class DidacticMaterialBookSelectionPanelComponent {
  public readonly panelEyebrow = input<string>('Gekoppelde boeken');
  public readonly panelTitle = input<string>('Zoek en selecteer boeken');
  public readonly hasSelectedUser = input<boolean>(true);
  public readonly searchQuery = input.required<string>();
  public readonly searchIsbn = input.required<string>();
  public readonly isLoadingBooks = input.required<boolean>();
  public readonly hasBookSearchInput = input.required<boolean>();
  public readonly filteredBooks = input.required<BookCandidate[]>();
  public readonly selectedBooksCount = input.required<number>();
  public readonly selectedBookIds = input.required<number[]>();

  public readonly searchQueryChanged = output<Event>();
  public readonly isbnSearchChanged = output<Event>();
  public readonly isbnKeydown = output<KeyboardEvent>();
  public readonly addBookRequested = output<BookCandidate>();

  protected isBookAlreadySelected(bookId: number): boolean {
    return this.selectedBookIds().includes(bookId);
  }
}
