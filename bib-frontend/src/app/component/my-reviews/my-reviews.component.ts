import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReviewService } from '../../services/Reviews/review.service';
import { Review } from '../../model/review.model';
import { RouterModule } from '@angular/router';
import { LucideAngularModule, Star, Trash2, Search } from 'lucide-angular';

@Component({
  selector: 'app-my-reviews',
  standalone: true,
  imports: [CommonModule, RouterModule, LucideAngularModule],
  templateUrl: './my-reviews.component.html',
  styleUrl: './my-reviews.component.css'
})
export class MyReviewsComponent implements OnInit {
  readonly Star = Star;
  readonly Trash2 = Trash2;
  readonly Search = Search;

  starsArray = [1, 2, 3, 4, 5];
  reviews: Review[] = [];
  private reviewService = inject(ReviewService);

  ngOnInit(): void {
    this.loadReviews();
  }

  loadReviews() {
    this.reviewService.getAllReviews().subscribe({
      next: (data) => {
        // Sorteer dalend op ID, zodat de meest recente reviews bovenaan staan
        this.reviews = data.sort((a, b) => (b.id || 0) - (a.id || 0));
      },
      error: (err) => {
        console.error('Error fetching reviews:', err);
      }
    });
  }

  deleteReview(reviewId: number | undefined) {
    if (!reviewId) return;
    this.reviewService.deleteReview(reviewId).subscribe({
      next: () => {
        this.reviews = this.reviews.filter(r => r.id !== reviewId);
      },
      error: (err) => {
        console.error('Error deleting review:', err);
      }
    });
  }
}
