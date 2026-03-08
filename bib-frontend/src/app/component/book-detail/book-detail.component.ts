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
  Bookmark
} from 'lucide-angular';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { BookService } from '../../services/BookService/book.service';
import { Availability } from '../../model/enums/availability.enum';
import { Review } from '../../model/review.model';
import { ReviewService } from '../../services/Reviews/review.service';
import { ReadingListService, ReadingList } from '../../services/reading-list.service';

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
  readonly Bookmark = Bookmark;
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

  //Reviews//
  reviews: Review[] = [];
  newReviewText: string = '';
  newReviewRating: number = 5;
  starsArray: number[] = [1, 2, 3, 4, 5];
  averageRating: number = 0;

  // Lists //
  readingLists: ReadingList[] = [];
  showListPicker: boolean = false;

  Availability = Availability;

  constructor(
    private route: ActivatedRoute,
    private bookService: BookService,
    private reviewService: ReviewService,
    private readingListService: ReadingListService
  ) {

  }

  ngOnInit() {
    this.route.paramMap.subscribe(params => {
      const id = params.get('id');

      if (id) {
        this.bookService.getBookById(+id).subscribe(book => {
          this.selectedBook = book;
        });

        this.loadReviews(+id);
      }
    });

    this.loadReadingLists();
  }

  public availabilityOptions = ['AVAILABLE', 'DAMAGED', 'LOST', 'BORROWED'];
  selectedBook?: Book;

  loadReviews(bookId: number) {
    this.reviewService.getReviewsForBook(bookId).subscribe({
      next: (data) => this.reviews = data,
      error: (err) => console.error('Fout bij ophalen reviews', err)
    });

    this.updateAverage(bookId);
  }

  loadReadingLists() {
    this.readingListService.getLists().subscribe((lists) => {
      this.readingLists = lists;
    });
  }

  toggleListPicker() {
    this.showListPicker = !this.showListPicker;
  }

  addBookToList(listId: number) {
    if (this.selectedBook?.id) {
      this.readingListService.addBookToList(listId, this.selectedBook.id).subscribe({
        next: () => {
          alert(`Boek toegevoegd aan leeslijst!`);
          this.showListPicker = false;
        },
        error: (err) => {
          if (err.error === 'Boek zit al in deze leeslijst') {
            alert("Dit boek staat al in deze leeslijst!");
          } else {
            console.error("Fout bij toevoegen aan leeslijst", err);
            alert("Er is een fout opgetreden bij het toevoegen aan de leeslijst.");
          }
        }
      });
    }
  }

  setRating(rating: number) {
    this.newReviewRating = rating;
  }

  getRoundedAverage(): number {
    return Math.round(this.averageRating);
  }

  updateAverage(bookId: number) {
    this.reviewService.getAverageRating(bookId).subscribe({
      next: (avg) => this.averageRating = avg,
      error: (err) => console.error('Fout bij ophalen gemiddelde', err)
    });
  }

  submitReview() {
    const bookId = this.selectedBook?.id;

    if (!this.newReviewText.trim() || !bookId) return;

    const newReview: Review = {
      comment: this.newReviewText,
      rating: this.newReviewRating
    };

    this.reviewService.addReviewToBook(bookId, newReview).subscribe({
      next: (savedReview) => {
        this.reviews.push(savedReview);
        this.newReviewText = '';
        this.newReviewRating = 5;

        this.updateAverage(bookId);
      },
      error: (err) => console.error('Fout bij opslaan review', err)
    });
  }

  deleteReview(reviewId?: number) {

    if (!reviewId) return;

    if (confirm('Weet je zeker dat je deze review wilt verwijderen?')) {

      this.reviewService.deleteReview(reviewId).subscribe({
        next: () => {
          this.reviews = this.reviews.filter(r => r.id !== reviewId);

          if (this.selectedBook?.id) {
            this.updateAverage(this.selectedBook.id)
          }
        },
        error: (err) => console.error('Fout bij verwijderen review', err)
      })
    }
  }

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
