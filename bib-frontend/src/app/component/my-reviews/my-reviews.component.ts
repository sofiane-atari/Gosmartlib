import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { LucideAngularModule, Star, Trash2, Search } from 'lucide-angular';

import { ReviewService } from '../../services/review-service/review.service';
import { Review } from '../../model/review.model';
import { ToastService } from '../../services/toast/toast.service';

@Component({
  selector: 'app-my-reviews',
  standalone: true,
  imports: [CommonModule, RouterModule, LucideAngularModule],
  templateUrl: './my-reviews.component.html',
  styleUrl: './my-reviews.component.css',
})
export class MyReviewsComponent implements OnInit {
  protected readonly Star = Star;
  protected readonly Trash2 = Trash2;
  protected readonly Search = Search;

  protected readonly starValues = [1, 2, 3, 4, 5];
  protected reviews: Review[] = [];

  private readonly reviewService = inject(ReviewService);
  private readonly toastService = inject(ToastService);

  public ngOnInit(): void {
    this.loadMyReviews();
  }

  protected loadMyReviews(): void {
    this.reviewService.getMyReviews().subscribe({
      next: (reviews) => {
        this.reviews = [...reviews].sort((firstReview, secondReview) =>
          (secondReview.id ?? 0) - (firstReview.id ?? 0),
        );
      },
      error: (error) => {
        console.error('Fout bij ophalen van mijn reviews:', error);
        this.toastService.show('Je reviews konden niet geladen worden.', 'error');
      },
    });
  }

  protected deleteReview(reviewId?: number): void {
    if (!reviewId) {
      return;
    }

    this.toastService.showConfirm(
      'Weet je zeker dat je deze review wilt verwijderen?',
      () => {
        this.reviewService.deleteReview(reviewId).subscribe({
          next: () => {
            this.reviews = this.reviews.filter((review) => review.id !== reviewId);
            this.toastService.show('Review verwijderd.', 'success');
          },
          error: (error) => {
            console.error('Fout bij verwijderen van review:', error);
            this.toastService.show('De review kon niet verwijderd worden.', 'error');
          },
        });
      },
      () => {}
    );
  }
}