import { Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ReadingLevel } from '../../model/readingLevel.model';

@Injectable({
  providedIn: 'root'
})
export class ReadingLevelsService {
  private apiUrl = `${environment.apiUrl}/reading-levels`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<ReadingLevel[]> {
    return this.http.get<ReadingLevel[]>(this.apiUrl);
  }
}
