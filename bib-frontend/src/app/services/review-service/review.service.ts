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

  public getReviewsForBook(bookId: number) {
    return this.http.get<Review[]>(`${this.apiUrl}/book/${bookId}`);
  }

  public getAllReviews() {
    return this.http.get<Review[]>(this.apiUrl);
  }

  public getFlaggedReviews() {
    return this.http.get<Review[]>(`${this.apiUrl}/flagged`);
  }

  public addReviewToBook(bookId: number, review: Review) {
    return this.http.post<Review>(`${this.apiUrl}/book/${bookId}`, review);
  }

  public getAverageRating(bookId: number) {
    return this.http.get<number>(`${this.apiUrl}/book/${bookId}/average`);
  }

  public deleteReview(reviewId: number) {
    return this.http.delete<void>(`${this.apiUrl}/${reviewId}`);
  }

  public reportReview(reviewId: number) {
    return this.http.post<Review>(`${this.apiUrl}/${reviewId}/report`, {});
  }

  public keepReview(reviewId: number) {
    return this.http.patch<Review>(`${this.apiUrl}/${reviewId}/keep`, {});
  }

}
