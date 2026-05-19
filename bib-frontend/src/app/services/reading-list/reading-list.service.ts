import { Injectable } from '@angular/core';
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";

// wat de backend geeft
export interface ReadingListBook {
  id: number;
  bookId: number;
}

export type ReadingListTargetType = 'PERSONAL' | 'CLASS' | 'SYSTEM';

export interface ReadingList {
  id: number;
  name: string;
  targetType?: ReadingListTargetType;
  targetValue?: string | null;
  books?: ReadingListBook[];
}

// wat je naar de backend stuurt
export interface CreateReadingListDTO {
  name: string;
  targetType?: ReadingListTargetType;
  targetValue?: string | null;
}

export interface TeacherAudienceOptions {
  classes: string[];
  years: string[];
  schools: string[];
}

export interface ReadingListShare {
  publicShareId: string;
  url: string;
}

import { environment } from '../../../environments/environment';

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
}
