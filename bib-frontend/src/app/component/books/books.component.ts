import { Component, OnInit } from '@angular/core';
import { Book } from '../../model/book.model';
import { BookService } from '../../services/book.service';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
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

@Component({
  selector: 'app-books',
  standalone: true,
  imports: [FormsModule, CommonModule, LucideAngularModule],
  templateUrl: './books.component.html',
  styleUrl: './books.component.css'
})
export class BooksComponent implements OnInit {
  readonly Book = BookPlus;
  readonly Heart = Heart;
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
    { icon: BookOpen, label: 'Catalogus' },
    { icon: Heart, label: 'Favorieten' },
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


  books: Book[] = [];
  selectedBook?: Book;
  showModal = false;
  public availabilityOptions = ['AVAILABLE', 'DAMAGED', 'LOST', 'BORROWED'];

  constructor(private bs: BookService, private router: Router){}

  ngOnInit(): void {
    this.bs.getBooks().subscribe(data => {this.books = data;});
  }
  openModal(book: Book){
    this.selectedBook = book;
    this.showModal = true;
  }
  closeModal(){
    this.selectedBook = undefined;
    this.showModal = false;
  }

  toggleFavorite(event: Event, book: Book){}

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
