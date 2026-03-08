import { Component, OnInit } from '@angular/core';
import { Book } from '../../model/book.model';
import { BookService } from '../../services/BookService/book.service';
import { ReadingListService } from '../../services/reading-list.service';
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

  icons = [
    { icon: BookOpen, label: 'Catalogus' },
    { icon: Heart, label: 'Favorieten' },
  ]


  books: Book[] = [];

  constructor(private bs: BookService, private router: Router, private readingListService: ReadingListService) { }

  ngOnInit(): void {
    this.bs.getBooks().subscribe(data => { this.books = data; });
  }


  toggleFavorite(event: Event, book: Book) {
    event.stopPropagation();
    if (book.id) {
      this.readingListService.addBookToFavorites(book.id).subscribe({
        next: () => {
          alert(`"${book.title}" is toegevoegd aan je Favorieten!`);
        },
        error: (err) => {
          if (err.error === 'Boek zit al in deze leeslijst') {
            alert(`"${book.title}" staat al in je Favorieten!`);
          } else {
            console.error("Fout bij toevoegen aan favorieten", err);
            alert("Er is een fout opgetreden bij het toevoegen aan favorieten.");
          }
        }
      });
    }
  }

  toBookDetailPage(book: Book) {
    this.router.navigate(['/book', book.id])
  }

}
