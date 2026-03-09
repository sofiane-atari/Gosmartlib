import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Book } from '../../model/book.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class BookService {

  private apiUrl = `${environment.apiUrl}/books`
  constructor(private http: HttpClient) { }

  getBooks(): Observable<Book[]> {
    return this.http.get<Book[]>(this.apiUrl)
  }
  
  getBookById(id: number): Observable<Book> {
  return this.http.get<Book>(`${this.apiUrl}/${id}`); 
  }
  addBook(book: Book): Observable<Book>{
    return this.http.post<Book>(this.apiUrl, book);
  }
  getBookByISBN(isbn: string){
   return this.http.get<Book>(`${this.apiUrl}/isbn/${isbn}`);
  }

  getYearRange(): Observable<{min: number, max: number}> {
    return this.http.get<{min: number, max: number}>(`${this.apiUrl}/years/range`);
  }

  getMaxPages(): Observable<number> {
    return this.http.get<number>(`${this.apiUrl}/pages/max`);
  }
}
