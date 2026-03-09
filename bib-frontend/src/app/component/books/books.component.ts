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
  Search
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
  readonly Search = Search;

  icons = [
    { icon: BookOpen, label: 'Catalogus' },
    { icon: Heart, label: 'Favorieten' },
  ]


  books: Book[] = [];
  filteredBooks: Book[] = [];
  absoluteMinYear: number = 0;
  absoluteMaxYear: number = 0;
  absoluteMaxPages: number = 0;

  filters = {
    searchTerm: '',
    minYear: null as number | null,
    maxYear: null as number | null,
    maxPages: null as number | null,
    sortBy: 'az'
  };

  constructor(private bs: BookService, private router: Router, private readingListService: ReadingListService) { }

  ngOnInit(): void {

    this.bs.getMaxPages().subscribe(max => {
      this.absoluteMaxPages = max;
      this.filters.maxPages = max;
    })

    this.bs.getYearRange().subscribe(range => {
      this.absoluteMinYear = range.min;
      this.absoluteMaxYear = range.max;
      
      this.filters.minYear = range.min;
      this.filters.maxYear = range.max;
    });

    this.bs.getBooks().subscribe(data => { 
      this.books = data; 
      this.filteredBooks = data;
      this.applyFilters();
    });
  }

  applyFilters() {
    this.filteredBooks = this.books.filter(book => {
      
      const searchMatch = !this.filters.searchTerm || 
        book.title.toLowerCase().includes(this.filters.searchTerm.toLowerCase()) || 
        book.author.toLowerCase().includes(this.filters.searchTerm.toLowerCase());

      const maxPagesMatch = !this.filters.maxPages || book.pages <= this.filters.maxPages;

      const minYearMatch = !this.filters.minYear || book.releaseYear >= this.filters.minYear;
      const maxYearMatch = !this.filters.maxYear || book.releaseYear <= this.filters.maxYear;

      return searchMatch && minYearMatch && maxYearMatch && maxPagesMatch;
    });

    
    this.sortBooks();
  }

  resetFilters() {
    this.filters.searchTerm = '';
    this.filters.minYear = this.absoluteMinYear;
    this.filters.maxYear = this.absoluteMaxYear;
    this.filters.maxPages = this.absoluteMaxPages;
    this.filters.sortBy = 'az'; 

    this.applyFilters();
  }

  sortBooks() {
    if (this.filters.sortBy === 'az') {
      this.filteredBooks.sort((a, b) => a.title.localeCompare(b.title));
    } else if (this.filters.sortBy === 'za') {
      this.filteredBooks.sort((a, b) => b.title.localeCompare(a.title));
    } else if (this.filters.sortBy === 'newest') {
      this.filteredBooks.sort((a, b) => b.releaseYear - a.releaseYear);
    }
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
