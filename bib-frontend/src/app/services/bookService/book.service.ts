import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { fetchEventSource } from '@microsoft/fetch-event-source';
import { AuthService } from '../authorization/auth.service';
import { Book, BookCondition } from '../../model/book/book.model';
import { BookPreview } from '../../model/book/book-preview.model';
import { environment } from '../../../environments/environment';
import { PaginatedResponse } from '../../model/shared/paging/PaginatedResponse.model';
import {
  BookAvailabilityFilter,
  BookDidacticFilter,
  BookSortOption,
} from '../../model/book/bookFilter.model';
import { BookCandidate } from '../../model/book/book-candidate.model';
import {
  BookLessonTipPreview,
  LessonTipPreview,
} from '../../model/lesson-tip/lesson-tip.model';

@Injectable({
  providedIn: 'root',
})
export class BookService {
  private apiUrl = `${environment.apiUrl}/books`;

  private authService = inject(AuthService);

  constructor(private http: HttpClient) {}

  getBooks(): Observable<Book[]> {
    return this.http.get<Book[]>(this.apiUrl);
  }

  getFilteredBooks(
    searchTerm: string | null,
    availability: BookAvailabilityFilter,
    minPages: number | null,
    maxPages: number | null,
    readingLevelId: number | null,
    genreName: string | null,
    didactic: BookDidacticFilter,
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

    if (readingLevelId !== null && Number.isFinite(readingLevelId)) {
      params = params.set('readingLevelId', readingLevelId);
    }

    if (genreName && genreName.trim().length > 0) {
      params = params.set('genreName', genreName.trim());
    }

    if (didactic === 'DIDACTIC') {
      params = params.set('didactic', 'true');
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

  public getBookByISBN(isbn: string): Observable<Book> {
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
    const params = new HttpParams().set('limit', limit);
    return this.http.get<Book[]>(`${this.apiUrl}/most-read`, { params });
  }

  getNewestBooks(limit: number = 3): Observable<Book[]> {
    const params = new HttpParams().set('limit', limit);
    return this.http.get<Book[]>(`${this.apiUrl}/newest`, { params });
  }

  uploadBooks(file: File, campusId: number): Observable<any> {
    const formData = new FormData();
    formData.append('file', file);

    return this.http.post(`${this.apiUrl}/upload`, formData, {
      params: { campusId },
    });
  }

  validateBooksStream(
    tempFileId: string,
    onMessage: (event: { event: string; data: string }) => void,
    onError: (error: any) => void,
    onOpen?: (response: Response) => Promise<void>,
  ): AbortController {
    const abortController = new AbortController();
    const token = this.authService.getValidToken();

    if (!token) {
      onError(new Error('Geen geldige token'));
      return abortController;
    }

    fetchEventSource(`${this.apiUrl}/upload/validate-stream/${tempFileId}`, {
      method: 'GET',
      headers: {
        Authorization: `Bearer ${token}`,
        Accept: 'text/event-stream',
      },
      signal: abortController.signal,
      onopen: onOpen,
      onmessage: onMessage,
      onerror: onError,
    });

    return abortController;
  }

  searchBooksInDb(
    title?: string,
    author?: string,
    releaseYear?: number,
  ): Observable<BookCandidate[]> {
    let params = new HttpParams();

    if (title?.trim()) {
      params = params.set('title', title.trim());
    }

    if (author?.trim()) {
      params = params.set('author', author.trim());
    }

    if (releaseYear != null) {
      params = params.set('releaseYear', releaseYear);
    }

    return this.http.get<BookCandidate[]>(`${this.apiUrl}/search`, { params });
  }

  searchBookCandidates(
    title: string,
    author: string,
    releaseYear: number,
  ): Observable<BookCandidate[]> {
    return this.http.get<BookCandidate[]>(`${this.apiUrl}/search-candidates`, {
      params: { title, author, releaseYear },
    });
  }

  selectBook(isbn13: string, campusId: number): Observable<Book> {
    return this.http.post<Book>(`${this.apiUrl}/select`, null, {
      params: {
        isbn13,
        campusId,
      },
    });
  }

  public addCopy(bookId: number, campusId?: number | null): Observable<Book> {
    const options = campusId ? { params: { campusId } } : {};
    return this.http.post<Book>(`${this.apiUrl}/${bookId}/copies`, {}, options);
  }

  public updateBookCopyCondition(
    copyId: number,
    condition: BookCondition,
  ): Observable<{ bookCopyId: number; condition: BookCondition }> {
    return this.http.patch<{ bookCopyId: number; condition: BookCondition }>(
      `${environment.apiUrl}/book-copies/${copyId}/condition`,
      { condition },
    );
  }

  public deleteBookCopy(copyId: number): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/book-copies/${copyId}`);
  }

  public deleteAllBookCopies(bookId: number): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/book-copies/book/${bookId}`);
  }

  getRelatedBooksCarousel(
    bookId: number,
    limit: number = 10,
  ): Observable<Book[]> {
    return this.http.get<Book[]>(
      `${this.apiUrl}/${bookId}/related-carousel?limit=${limit}`,
    );
  }

  public getLessonTipsByBookId(bookId: number): Observable<LessonTipPreview[]> {
    return this.http.get<LessonTipPreview[]>(
      `${this.apiUrl}/${bookId}/lesson-tips`,
    );
  }

  public getLessonTipPreviewByBookId(
    bookId: number,
  ): Observable<BookLessonTipPreview> {
    return this.http.get<BookLessonTipPreview>(
      `${this.apiUrl}/${bookId}/lesson-tips/preview`,
    );
  }
}
