import { HttpClient, HttpHeaders } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';
import { Review } from '../../model/review.model';
import { Observable } from 'rxjs';
import { PaginatedResponse } from '../../model/shared/paging/PaginatedResponse.model';

@Injectable({
  providedIn: 'root'
})
export class ReviewService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/reviews`;

  public getReviewsForBook(bookId: number) {
    return this.http.get<Review[]>(`${this.apiUrl}/book/${bookId}`);
  }

  public getAllReviews() {
    return this.http.get<Review[]>(this.apiUrl);
  }

  public getMyReviews(page: number, size: number): Observable<PaginatedResponse<Review>> {
    return this.http.get<PaginatedResponse<Review>>(
      `${this.apiUrl}/me?page=${page}&size=${size}`,
    );
  }

  public getFlaggedReviews(page: number, size: number): Observable<PaginatedResponse<Review>> {
    return this.http.get<PaginatedResponse<Review>>(
      `${this.apiUrl}/flagged?page=${page}&size=${size}`,
    );
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
