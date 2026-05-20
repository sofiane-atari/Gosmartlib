import { Injectable } from '@angular/core';
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";
import { environment } from '../../../environments/environment';
import { CreateReadingListDTO, ReadingList, ReadingListBook, ReadingListShare, TeacherAudienceOptions } from '../../model/readingList.model';

@Injectable({
  providedIn: 'root'
})
export class ReadingListService {
  private apiUrl = `${environment.apiUrl}/reading-lists`;

  constructor(private http: HttpClient) { }

  createList(nameOrDto: string | CreateReadingListDTO): Observable<ReadingList> {
    const dto: CreateReadingListDTO = typeof nameOrDto === 'string'
      ? { name: nameOrDto }
      : nameOrDto;

    return this.http.post<ReadingList>(this.apiUrl, dto);
  }

  getTeacherAudienceOptions(): Observable<TeacherAudienceOptions> {
    return this.http.get<TeacherAudienceOptions>(`${this.apiUrl}/teacher/audience-options`);
  }

  getLists(): Observable<ReadingList[]> {
    return this.http.get<ReadingList[]>(this.apiUrl);
  }

  deleteList(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  getListById(id: number): Observable<ReadingList> {
    return this.http.get<ReadingList>(`${this.apiUrl}/${id}`);
  }

  addBookToList(listId: number, bookId: number): Observable<ReadingListBook> {
    return this.http.post<ReadingListBook>(`${this.apiUrl}/${listId}/books`, { bookId });
  }

  addBookToFavorites(bookId: number): Observable<ReadingListBook> {
    return this.http.post<ReadingListBook>(`${this.apiUrl}/favorites/books`, { bookId });
  }

  removeBookFromList(listId: number, bookId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${listId}/books/${bookId}`);
  }

  shareReadingList(listId: number): Observable<ReadingListShare> {
    return this.http.post<ReadingListShare>(
      `${this.apiUrl}/${listId}/share`,
      null,
      {
        params: {
          frontendBaseUrl: window.location.origin
        }
      }
    );
  }

  getSharedList(publicShareId: string): Observable<ReadingList> {
    return this.http.get<ReadingList>(
      `${this.apiUrl}/shared/${publicShareId}`
    );
  }

  getFeaturedListsForCampus(): Observable<ReadingList[]> {
    return this.http.get<ReadingList[]>(`${this.apiUrl}/featured`);
  }
}
