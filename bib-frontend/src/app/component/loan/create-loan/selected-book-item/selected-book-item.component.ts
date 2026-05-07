import { CommonModule } from '@angular/common';
import { Component, input, output } from '@angular/core';
import { Book } from '../../../../model/book/book.model';

@Component({
  selector: 'app-selected-book-item',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './selected-book-item.component.html',
  styleUrl: './selected-book-item.component.css',
})
export class SelectedBookItemComponent {
  public readonly book = input.required<Book>();
  public readonly showRemove = input<boolean>(false);
  public readonly removeRequested = output<number | undefined>();
  public readonly quantity = input<number>(1);
}
