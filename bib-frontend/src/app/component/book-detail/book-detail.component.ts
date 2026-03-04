import { Component } from '@angular/core';
import { Book } from '../../model/book.model';
import {
  LucideAngularModule,
  Heart,
  BookOpen,
  X,
  Library,
  Minus,
  Plus,
  CheckCircle,
  Edit2,
  Trash2,
  Calendar,
  Star,
  MessageCircle,
  BookPlus,
} from 'lucide-angular';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { BookService } from '../../services/book.service';

@Component({
  selector: 'app-book-detail',
  standalone: true,
  imports: [LucideAngularModule, CommonModule, FormsModule],
  templateUrl: './book-detail.component.html',
  styleUrl: './book-detail.component.css'
})
export class BookDetailComponent {
  readonly X = X;
  readonly Library = Library;
  readonly Minus = Minus;
  readonly Plus = Plus;
  readonly CheckCircle = CheckCircle;
  readonly Edit2 = Edit2;
  readonly Trash2 = Trash2;
  readonly Calendar = Calendar;
  readonly Star = Star;
  readonly MessageCircle = MessageCircle;
  readonly BookOpen = BookOpen;
  icons = [
    { icon: Library, label: 'Bibliotheek' },
    { icon: Star, label: 'Reviews' },
    { icon: MessageCircle, label: 'Berichten' },
    { icon: Calendar, label: 'Planning' },
    { icon: CheckCircle, label: 'Voltooid' },
    { icon: Edit2, label: 'Bewerken' },
    { icon: Plus, label: 'Toevoegen' },
    { icon: Minus, label: 'Verwijderen' },
    { icon: Trash2, label: 'Prullenbak' },
    { icon: X, label: 'Sluiten' },
  ];

  constructor(  private route: ActivatedRoute,
  private bookService: BookService) {}

  ngOnInit() {
    this.route.paramMap.subscribe(params => {
      const id = params.get('id');

      if (id) {
        this.bookService.getBookById(+id).subscribe(book => {
          this.selectedBook = book;
        });
      }
    });
  }

  public availabilityOptions = ['AVAILABLE', 'DAMAGED', 'LOST', 'BORROWED'];
  selectedBook?: Book;

  
  increaseQuantity(): void {
    if (this.selectedBook) {
      this.selectedBook.copies = (this.selectedBook.copies || 0) + 1;
    }
  }

  decreaseQuantity(): void {
    if (this.selectedBook && (this.selectedBook.copies || 0) > 0) {
      this.selectedBook.copies = (this.selectedBook.copies || 0) - 1;
    }
  }
}
