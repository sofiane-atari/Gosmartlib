import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Book } from '../../model/book.model';
import { environment } from '../../../environments/environment';
import { PaginatedResponse } from '../../model/paginated-response.model.ts';
import { BookAvailabilityFilter, BookSortOption } from '../../model/bookFilter.model';

@Injectable({
  providedIn: 'root',
})
export class BookService {
  private apiUrl = `${environment.apiUrl}/books`;
  constructor(private http: HttpClient) {}

  getBooks(): Observable<Book[]> {
    return this.http.get<Book[]>(this.apiUrl);
  }

  getFilteredBooks(
    searchTerm: string | null,
    availability: BookAvailabilityFilter,
    minPages: number | null,
    maxPages: number | null,
    page: number,
    size: number,
    sortBy: BookSortOption,
  ): Observable<PaginatedResponse<Book>> {
    let params = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('sortBy', sortBy);

    if (searchTerm && searchTerm.trim().length > 0) {
      params = params.set('searchTerm', searchTerm.trim());
    }

    if (availability !== 'ALL') {
      params = params.set('availability', availability);
    }

    if (minPages !== null) {
      params = params.set('minPages', minPages);
    }

    if (maxPages !== null) {
      params = params.set('maxPages', maxPages);
    }

    return this.http.get<PaginatedResponse<Book>>(`${this.apiUrl}/catalog`, {
      params,
    });
  }

  getBookById(id: number): Observable<Book> {
    return this.http.get<Book>(`${this.apiUrl}/${id}`);
  }
  addBook(book: Book): Observable<Book> {
    return this.http.post<Book>(this.apiUrl, book);
  }
  getBookByISBN(isbn: string) {
    return this.http.get<Book>(`${this.apiUrl}/isbn/${isbn}`);
  }

  getMaxPages(): Observable<number> {
    return this.http.get<number>(`${this.apiUrl}/pages/max`);
  }

  getMinPages(): Observable<number> {
    return this.http.get<number>(`${this.apiUrl}/pages/min`);
  }

  getMostReadBooks(limit: number = 3): Observable<Book[]> {
    return this.http.get<Book[]>(`${this.apiUrl}/most-read?limit=${limit}`);
  }

  getNewestBooks(limit: number = 3): Observable<Book[]> {
    return this.http.get<Book[]>(`${this.apiUrl}/newest?limit=${limit}`);
  }
}
