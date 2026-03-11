import { Component, Input, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { Book } from '../../model/book.model';
import { LucideAngularModule, BookOpen } from 'lucide-angular';

@Component({
  selector: 'app-book-list-sidebar',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './book-list-sidebar.component.html',
  styleUrl: './book-list-sidebar.component.css',
})
export class BookListSidebarComponent {
  private readonly router = inject(Router);

  @Input() title: string = '';
  @Input() books: Book[] = [];
  @Input() loading: boolean = false;

  readonly BookOpen = BookOpen;

  navigateToBook(book: Book): void {
    if (book.id) {
      this.router.navigate(['/book', book.id]);
    }
  }
}
