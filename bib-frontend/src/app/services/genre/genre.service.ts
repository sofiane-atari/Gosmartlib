import { PageingOpt } from '../../model/shared/paging/PageingOpt.model';
import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';
import { Genre, GenreStats } from '../../model/genre.model';
import { Observable } from 'rxjs';
import { PaginatedResponse } from '../../model/shared/paging/PaginatedResponse.model';

export interface genreGetOpt {
  includeBookCount?: boolean;
  bookCount?: string;
  search?: string;
}

export interface genreGetOptWithPaging {
  genreGetOpt?: genreGetOpt;
  pagingOpt: PageingOpt;
}

@Injectable({
  providedIn: 'root',
})
export class GenreService {
  private http = inject(HttpClient);
  private url = `${environment.apiUrl}/genres`;

  public createGenre(genre: Genre): Observable<Genre> {
    return this.http.post<Genre>(this.url, genre);
  }
  getGenres() {
    return this.http.get<Genre[]>(`${this.url}/all`);
  }

  public getGenresByPage(
    opt: genreGetOptWithPaging,
  ): Observable<PaginatedResponse<Genre>> {
    let params = new HttpParams({
      fromObject: {
        includeBookCount: String(opt.genreGetOpt?.includeBookCount ?? false),
        page: String(opt.pagingOpt.page),
        pageSize: String(opt.pagingOpt.pageSize),
      },
    });

    // Add search parameter if provided
    if (opt.genreGetOpt?.search) {
      params = params.set('search', opt.genreGetOpt.search);
    }

    // Add bookCount filter if provided
    if (opt.genreGetOpt?.bookCount) {
      params = params.set('bookCount', opt.genreGetOpt.bookCount);
    }

    return this.http.get<PaginatedResponse<Genre>>(this.url, { params });
  }

  public getGenreStats(): Observable<GenreStats> {
    return this.http.get<GenreStats>(`${this.url}/_stats`);
  }

  public deleteGenreByName(name: string) {
    return this.http.delete<void>(`${this.url}/${encodeURIComponent(name)}`);
  }

  public updateGenre(genre: Genre): Observable<Genre> {
    return this.http.put<Genre>(
      `${this.url}/${encodeURIComponent(genre.name)}`,
      genre,
    );
  }
}