import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Book } from '../../model/book/book.model';
import { BookPreview } from '../../model/book/book-preview.model';
import { environment } from '../../../environments/environment';
import { PaginatedResponse } from '../../model/shared/paging/PaginatedResponse.model'
import { BookAvailabilityFilter, BookSortOption } from '../../model/book/bookFilter.model';
import { BookCandidate } from '../../model/book/book-candidate.model';


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
      .set('page', Math.max(0, page))
      .set('size', Math.max(1, size))
      .set('sortBy', sortBy);

    if (searchTerm && searchTerm.trim().length > 0) {
      params = params.set('searchTerm', searchTerm.trim());
    }

    if (availability !== 'ALL') {
      params = params.set('availability', availability);
    }

    if (minPages !== null && Number.isFinite(minPages)) {
      params = params.set('minPages', minPages);
    }

    if (maxPages !== null && Number.isFinite(maxPages)) {
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

  public updateBook(id: number, book: Book): Observable<Book> {
    return this.http.put<Book>(`${this.apiUrl}/${id}`, book);
  }

  public deleteBook(id: number) {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  getBookByISBN(isbn: string) {
    return this.http.get<Book>(`${this.apiUrl}/isbn/${isbn}`);
  }

  getBookPreviewByISBN(isbn: string): Observable<BookPreview> {
    return this.http.get<BookPreview>(`${this.apiUrl}/isbn/${isbn}/preview`);
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

  uploadBooks(file: File): Observable<any>{
    const formData = new FormData();
    formData.append("file", file);

    return this.http.post(`${this.apiUrl}/upload`, formData)
  }

  validateBooksStream(tempFileId: string): EventSource {
    return new EventSource(`${this.apiUrl}/upload/validate-stream/${tempFileId}`);
  }

  searchBookCandidates(title: string, author: string, releaseYear: number): Observable<BookCandidate[]> {
    return this.http.get<BookCandidate[]>(`${this.apiUrl}/search`, {
      params: {
        title,
        author,
        releaseYear
      }
    });
  }

  selectBook(isbn13: string): Observable<Book> {
    return this.http.post<Book>(`${this.apiUrl}/select`, null, {
      params: {
        isbn13
      }
    });
  }



  getRelatedBooksCarousel(bookId: number, limit: number = 10): Observable<Book[]> {
    return this.http.get<Book[]>(`${this.apiUrl}/${bookId}/related-carousel?limit=${limit}`);
  }
}
