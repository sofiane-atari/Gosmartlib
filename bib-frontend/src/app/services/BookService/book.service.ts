import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Book } from '../../model/book.model';
import { environment } from '../../../environments/environment';
import { FileValidationResult } from '../../model/FileValidationResult';
import { BookCandidate } from '../../model/book-candidate.model';

@Injectable({
  providedIn: 'root',
})
export class BookService {
  private apiUrl = `${environment.apiUrl}/books`;
  constructor(private http: HttpClient) {}

  getBooks(): Observable<Book[]> {
    return this.http.get<Book[]>(this.apiUrl);
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

  getYearRange(): Observable<{ min: number; max: number }> {
    return this.http.get<{ min: number; max: number }>(
      `${this.apiUrl}/years/range`,
    );
  }

  getMaxPages(): Observable<number> {
    return this.http.get<number>(`${this.apiUrl}/pages/max`);
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

}
