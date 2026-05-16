import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { MailSubscriptionDTO } from '../../model/mail-subscription/mail-subscription.model';

@Injectable({
  providedIn: 'root',
})
export class MailSubscriptionService {
  private apiUrl = `${environment.apiUrl}/subscriptions`;

  constructor(private http: HttpClient) {}

  subscribe(bookId: number): Observable<MailSubscriptionDTO> {
    return this.http.post<MailSubscriptionDTO>(
      `${this.apiUrl}/books/${bookId}`,
      {},
    );
  }

  unsubscribe(bookId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/books/${bookId}`);
  }

  getMySubscriptions(): Observable<MailSubscriptionDTO[]> {
    return this.http.get<MailSubscriptionDTO[]>(`${this.apiUrl}/me`);
  }
}
