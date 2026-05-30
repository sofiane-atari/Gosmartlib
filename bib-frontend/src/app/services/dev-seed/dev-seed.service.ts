import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

export interface DevSeedResult {
  tagsCreated: number;
  booksCreated: number;
  copiesCreated: number;
  loansCreated: number;
  loansReturned: number;
  reviewsCreated: number;
  reviewsReported: number;
  readingListsCreated: number;
  favoritesAdded: number;
  lessonTipsCreated: number;
  mailSubscriptionsCreated: number;
}

@Injectable({
  providedIn: 'root',
})
export class DevSeedService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/dev-seed`;

  triggerSeed(): Promise<DevSeedResult> {
    return new Promise((resolve, reject) => {
      this.http.post<DevSeedResult>(this.apiUrl, null).subscribe({
        next: (result) => resolve(result),
        error: (err) => reject(err),
      });
    });
  }

  clearSeed(): Promise<void> {
    return new Promise((resolve, reject) => {
      this.http.post<void>(`${this.apiUrl}/clear`, null).subscribe({
        next: () => resolve(),
        error: (err) => reject(err),
      });
    });
  }
}