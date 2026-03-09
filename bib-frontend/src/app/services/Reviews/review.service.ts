import { HttpClient, HttpHeaders } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';
import { Review } from '../../model/review.model';

@Injectable({
  providedIn: 'root'
})
export class ReviewService {
  private http = inject(HttpClient);

  private apiUrl = `${environment.apiUrl}/reviews`;

  getReviewsForBook(bookId: number) {
    return this.http.get<Review[]>(`${this.apiUrl}/book/${bookId}`);
  }

  addReviewToBook(bookId: number, review: Review) {
    return this.http.post<Review>(`${this.apiUrl}/book/${bookId}`, review);
  }

  getAverageRating(bookId: number) {
    return this.http.get<number>(`${this.apiUrl}/book/${bookId}/average`);
  }

  deleteReview(reviewId: number) {
    return this.http.delete(`${this.apiUrl}/${reviewId}`);
  }

  reportReview(reviewId: number) {
    return this.http.post<Review>(`${this.apiUrl}/${reviewId}/report`, {});
  }

}
